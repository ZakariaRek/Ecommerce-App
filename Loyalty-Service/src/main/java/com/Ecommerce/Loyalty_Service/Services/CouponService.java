package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Repositories.CouponRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CouponKafkaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.hibernate.StaleObjectStateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {
    private final CouponRepository couponRepository;
    private final CouponKafkaService kafkaService;
    private final PointTransactionService pointTransactionService;
    private final CRMService crmService;


    /**
     * Get all coupons from the repository
     */

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    /**
     * Get a coupon by its code
     */
    public Coupon getCouponByCode(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found with code: " + code));
    }
    /**
     * validatea coupon if it exists and is not used or expired
     *
     */
    public boolean validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found with code: " + code));

        boolean isValid = !coupon.isUsed() &&
                !coupon.getExpirationDate().isBefore(LocalDateTime.now());

        log.info("Coupon {} validation result: {}", code, isValid);
        return isValid;
    }

    /**
     * Enhanced coupon generation that costs points with proper error handling
     */
    @Transactional
    @Retryable(
            value = {
                    ObjectOptimisticLockingFailureException.class,
                    OptimisticLockingFailureException.class,
                    StaleObjectStateException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000)
    )
    public Coupon generateCouponForPoints(UUID userId, DiscountType discountType,
                                          BigDecimal discountValue, int pointsCost,
                                          BigDecimal minPurchaseAmount, BigDecimal maxDiscountAmount,
                                          LocalDateTime expirationDate, int usageLimit) {

        log.info("🎟️ Generating coupon for user {} costing {} points", userId, pointsCost);

        try {
            // Check if user has enough points
            var userCrm = crmService.getByUserId(userId);
            if (userCrm.getTotalPoints() < pointsCost) {
                throw new RuntimeException("Insufficient points. Required: " + pointsCost +
                        ", Available: " + userCrm.getTotalPoints());
            }

            // Deduct points first using idempotency
            String idempotencyKey = "coupon-gen-" + userId + "-" + System.currentTimeMillis();
            pointTransactionService.recordTransactionWithIdempotency(
                    userId,
                    TransactionType.REDEEM,
                    pointsCost,
                    "Coupon Generation: " + discountValue + " " + discountType,
                    idempotencyKey
            );

            // Generate the coupon - DON'T manually set ID, let JPA handle it
            Coupon coupon = createNewCoupon(userId, discountType, discountValue,
                    minPurchaseAmount, maxDiscountAmount,
                    expirationDate, usageLimit);

            // Save with retry logic
            Coupon savedCoupon = saveCouponSafely(coupon);

            // Publish event with point cost information
            kafkaService.publishCouponGenerated(savedCoupon, "POINTS_PURCHASE");

            log.info("✅ Generated coupon {} for user {} (cost: {} points)",
                    savedCoupon.getCode(), userId, pointsCost);

            return savedCoupon;

        } catch (Exception e) {
            log.error("❌ Error generating coupon for user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Helper method to create new coupon without setting ID
     */
    private Coupon createNewCoupon(UUID userId, DiscountType discountType,
                                   BigDecimal discountValue, BigDecimal minPurchaseAmount,
                                   BigDecimal maxDiscountAmount, LocalDateTime expirationDate,
                                   int usageLimit) {
        Coupon coupon = new Coupon();
        // DON'T set ID - let JPA generate it automatically
        // coupon.setId(UUID.randomUUID()); // Remove this line!

        coupon.setCode(generateCouponCode());
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setMinPurchaseAmount(minPurchaseAmount);
        coupon.setMaxDiscountAmount(maxDiscountAmount);
        coupon.setExpirationDate(expirationDate);
        coupon.setUserId(userId);
        coupon.setUsed(false);
        coupon.setUsageLimit(usageLimit);
        coupon.setStackable(true);
        coupon.setPriorityLevel(1);

        return coupon;
    }

    /**
     * Safe coupon saving with retry logic
     */
    private Coupon saveCouponSafely(Coupon coupon) {
        try {
            return couponRepository.save(coupon);
        } catch (OptimisticLockingFailureException e) {
            log.warn("⚠️ Optimistic locking failure while saving coupon, retrying...");
            throw e; // Let @Retryable handle the retry
        }
    }

    /**
     * Recovery method when all retries are exhausted
     */
    @Recover
    public Coupon recoverFromOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex,
                                                      UUID userId, DiscountType discountType,
                                                      BigDecimal discountValue, int pointsCost,
                                                      BigDecimal minPurchaseAmount, BigDecimal maxDiscountAmount,
                                                      LocalDateTime expirationDate, int usageLimit) {
        return handleCouponGenerationRecovery(ex, userId, discountType, discountValue, pointsCost);
    }

    @Recover
    public Coupon recoverFromOptimisticLockingFailure(OptimisticLockingFailureException ex,
                                                      UUID userId, DiscountType discountType,
                                                      BigDecimal discountValue, int pointsCost,
                                                      BigDecimal minPurchaseAmount, BigDecimal maxDiscountAmount,
                                                      LocalDateTime expirationDate, int usageLimit) {
        return handleCouponGenerationRecovery(ex, userId, discountType, discountValue, pointsCost);
    }

    private Coupon handleCouponGenerationRecovery(Exception ex, UUID userId, DiscountType discountType,
                                                  BigDecimal discountValue, int pointsCost) {
        log.error("💀 CRITICAL: Failed to generate coupon after all retries for user {}", userId);
        log.error("💀 Coupon details - Type: {}, Value: {}, Cost: {} points",
                discountType, discountValue, pointsCost);
        log.error("💀 Original error: {}", ex.getMessage());

        // In production, you might want to:
        // 1. Refund the points that were deducted
        // 2. Store this in a failed operations queue for manual processing
        // 3. Send an alert to operations team

        throw new RuntimeException(
                String.format("Failed to generate coupon for user %s after retries. Points may have been deducted - please contact support.",
                        userId), ex);
    }

    /**
     * Predefined coupon packages with point costs
     */
    public enum CouponPackage {
        BASIC_5_PERCENT(150, DiscountType.PERCENTAGE, new BigDecimal("5.00"), new BigDecimal("25.00"), new BigDecimal("10.00")),
        STANDARD_10_PERCENT(300, DiscountType.PERCENTAGE, new BigDecimal("10.00"), new BigDecimal("50.00"), new BigDecimal("25.00")),
        PREMIUM_15_PERCENT(500, DiscountType.PERCENTAGE, new BigDecimal("15.00"), new BigDecimal("100.00"), new BigDecimal("50.00")),
        FIXED_5_DOLLAR(100, DiscountType.FIXED_AMOUNT, new BigDecimal("5.00"), new BigDecimal("30.00"), null),
        FIXED_10_DOLLAR(200, DiscountType.FIXED_AMOUNT, new BigDecimal("10.00"), new BigDecimal("50.00"), null),
        FIXED_25_DOLLAR(450, DiscountType.FIXED_AMOUNT, new BigDecimal("25.00"), new BigDecimal("100.00"), null);

        private final int pointsCost;
        private final DiscountType discountType;
        private final BigDecimal discountValue;
        private final BigDecimal minPurchaseAmount;
        private final BigDecimal maxDiscountAmount;

        CouponPackage(int pointsCost, DiscountType discountType, BigDecimal discountValue,
                      BigDecimal minPurchaseAmount, BigDecimal maxDiscountAmount) {
            this.pointsCost = pointsCost;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.minPurchaseAmount = minPurchaseAmount;
            this.maxDiscountAmount = maxDiscountAmount;
        }

        public int getPointsCost() { return pointsCost; }
        public DiscountType getDiscountType() { return discountType; }
        public BigDecimal getDiscountValue() { return discountValue; }
        public BigDecimal getMinPurchaseAmount() { return minPurchaseAmount; }
        public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    }

    /**
     * Generate coupon from predefined package
     */
    @Transactional
    public Coupon generateCouponFromPackage(UUID userId, CouponPackage packageType) {
        LocalDateTime expirationDate = LocalDateTime.now().plusMonths(3); // 3 months validity

        return generateCouponForPoints(
                userId,
                packageType.getDiscountType(),
                packageType.getDiscountValue(),
                packageType.getPointsCost(),
                packageType.getMinPurchaseAmount(),
                packageType.getMaxDiscountAmount(),
                expirationDate,
                1 // Single use
        );
    }

    /**
     * Get available coupon packages for user based on their points
     */
    public List<CouponPackage> getAvailableCouponPackages(UUID userId) {
        var userCrm = crmService.getByUserId(userId);
        int userPoints = userCrm.getTotalPoints();

        return java.util.Arrays.stream(CouponPackage.values())
                .filter(pkg -> userPoints >= pkg.getPointsCost())
                .toList();
    }

    // Keep existing methods for backward compatibility
    public String generateCouponCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public Coupon generateCoupon(UUID userId, DiscountType discountType, BigDecimal discountValue,
                                 BigDecimal minPurchaseAmount, BigDecimal maxDiscountAmount,
                                 LocalDateTime expirationDate, int usageLimit) {
        // This is now the FREE coupon generation (for admin use or promotions)
        log.info("🎁 Generating FREE promotional coupon for user {}", userId);

        Coupon coupon = createNewCoupon(userId, discountType, discountValue,
                minPurchaseAmount, maxDiscountAmount,
                expirationDate, usageLimit);

        Coupon savedCoupon = saveCouponSafely(coupon);
        kafkaService.publishCouponGenerated(savedCoupon, "PROMOTIONAL");

        log.info("🎁 Generated promotional coupon {} for user {}", savedCoupon.getCode(), userId);
        return savedCoupon;
    }

    @Transactional
    public Coupon generateRewardCoupon(UUID userId, DiscountType discountType, BigDecimal discountValue,
                                       int pointsCost) {
        // This method now properly deducts points
        BigDecimal minPurchaseAmount = new BigDecimal("25.00");
        BigDecimal maxDiscountAmount = discountType == DiscountType.PERCENTAGE ?
                new BigDecimal("100.00") : null;
        LocalDateTime expirationDate = LocalDateTime.now().plusMonths(3);

        return generateCouponForPoints(
                userId, discountType, discountValue, pointsCost,
                minPurchaseAmount, maxDiscountAmount, expirationDate, 1
        );
    }

    public boolean validateCoupon(String couponCode, BigDecimal purchaseAmount) {
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        boolean isValid = !coupon.isUsed() &&
                !coupon.getExpirationDate().isBefore(LocalDateTime.now()) &&
                purchaseAmount.compareTo(coupon.getMinPurchaseAmount()) >= 0;

        kafkaService.publishCouponValidated(
                couponCode,
                coupon.getUserId(),
                isValid,
                purchaseAmount,
                isValid ? "Coupon is valid" : "Coupon validation failed"
        );

        return isValid;
    }

    @Transactional
    @Retryable(
            value = {
                    ObjectOptimisticLockingFailureException.class,
                    OptimisticLockingFailureException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public BigDecimal applyCoupon(String couponCode, BigDecimal purchaseAmount) {
        if (!validateCoupon(couponCode, purchaseAmount)) {
            throw new RuntimeException("Invalid coupon");
        }

        Coupon coupon = couponRepository.findByCode(couponCode).get();

        BigDecimal discountAmount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = purchaseAmount.multiply(coupon.getDiscountValue().divide(new BigDecimal(100)));
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        if (coupon.getMaxDiscountAmount() != null &&
                discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discountAmount = coupon.getMaxDiscountAmount();
        }

        BigDecimal finalAmount = purchaseAmount.subtract(discountAmount);

        // Mark coupon as used
        coupon.setUsed(true);

        // Save with retry protection
        Coupon updatedCoupon = saveCouponSafely(coupon);

        kafkaService.publishCouponRedeemed(
                updatedCoupon,
                purchaseAmount,
                discountAmount,
                finalAmount,
                null
        );

        log.info("Coupon {} applied by user {}, discount amount: {}",
                couponCode, coupon.getUserId(), discountAmount);

        return finalAmount;
    }

    public List<Coupon> getUserActiveCoupons(UUID userId) {
        return couponRepository.findByUserIdAndIsUsedFalse(userId);
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void expireOutdatedCoupons() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Processing coupon expirations for coupons expired before: {}", now);
        // Implementation for expiring coupons would go here
    }
}