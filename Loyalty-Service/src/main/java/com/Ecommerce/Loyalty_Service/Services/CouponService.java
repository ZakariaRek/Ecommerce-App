package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import com.Ecommerce.Loyalty_Service.Repositories.CouponRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CouponKafkaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    public String generateCouponCode() {
        // Generate a random alphanumeric code
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
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode(generateCouponCode());
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setMinPurchaseAmount(minPurchaseAmount);
        coupon.setMaxDiscountAmount(maxDiscountAmount);
        coupon.setExpirationDate(expirationDate);
        coupon.setUserId(userId);
        coupon.setUsed(false);
        coupon.setUsageLimit(usageLimit);

        // Save the coupon (MongoDB listener will trigger event)
        Coupon savedCoupon = couponRepository.save(coupon);

        // Direct Kafka event with specific generation reason
        kafkaService.publishCouponGenerated(savedCoupon, "USER_REQUESTED");

        log.info("Generated coupon {} for user {}", savedCoupon.getCode(), userId);

        return savedCoupon;
    }

    /**
     * Generate a coupon as a reward for points redemption
     */
    @Transactional
    public Coupon generateRewardCoupon(UUID userId, DiscountType discountType, BigDecimal discountValue,
                                       int pointsCost) {
        // Calculate default values for reward coupons
        BigDecimal minPurchaseAmount = new BigDecimal("25.00"); // $25 minimum purchase
        BigDecimal maxDiscountAmount = discountType == DiscountType.PERCENTAGE ?
                new BigDecimal("100.00") : null; // $100 max discount for percentage coupons
        LocalDateTime expirationDate = LocalDateTime.now().plusMonths(3); // 3 month expiration
        int usageLimit = 1; // Single use

        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID());
        coupon.setCode(generateCouponCode());
        coupon.setDiscountType(discountType);
        coupon.setDiscountValue(discountValue);
        coupon.setMinPurchaseAmount(minPurchaseAmount);
        coupon.setMaxDiscountAmount(maxDiscountAmount);
        coupon.setExpirationDate(expirationDate);
        coupon.setUserId(userId);
        coupon.setUsed(false);
        coupon.setUsageLimit(usageLimit);

        // Save the coupon
        Coupon savedCoupon = couponRepository.save(coupon);

        // Direct Kafka event with specific generation reason
        kafkaService.publishCouponGenerated(savedCoupon, "POINTS_REDEMPTION");

        log.info("Generated reward coupon {} for user {} costing {} points",
                savedCoupon.getCode(), userId, pointsCost);

        return savedCoupon;
    }

    public boolean validateCoupon(String couponCode, BigDecimal purchaseAmount) {
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        // Check if coupon is valid
        boolean isValid = !coupon.isUsed() &&
                !coupon.getExpirationDate().isBefore(LocalDateTime.now()) &&
                purchaseAmount.compareTo(coupon.getMinPurchaseAmount()) >= 0;

        // Send validation event to Kafka
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

        // Apply max discount constraint
        if (coupon.getMaxDiscountAmount() != null &&
                discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discountAmount = coupon.getMaxDiscountAmount();
        }

        // Calculate final amount
        BigDecimal finalAmount = purchaseAmount.subtract(discountAmount);

        // Mark coupon as used
        coupon.setUsed(true);
        couponRepository.save(coupon);

        // Direct Kafka event with order details (assuming this is being called during checkout)
        // In a real implementation, you would have the order ID
        kafkaService.publishCouponRedeemed(
                coupon,
                purchaseAmount,
                discountAmount,
                finalAmount,
                null // Order ID would be available in a real implementation
        );

        log.info("Coupon {} applied by user {}, discount amount: {}",
                couponCode, coupon.getUserId(), discountAmount);

        return finalAmount;
    }

    public List<Coupon> getUserActiveCoupons(UUID userId) {
        return couponRepository.findByUserIdAndIsUsedFalse(userId);
    }

    /**
     * Scheduled task to expire outdated coupons
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run at 1:00 AM every day
    @Transactional
    public void expireOutdatedCoupons() {
        LocalDateTime now = LocalDateTime.now();
        // In a real implementation, you would query for expired coupons
        // For this example, we'll leave this as a placeholder

        log.info("Processing coupon expirations for coupons expired before: {}", now);

        // For each expired coupon, mark as expired and publish event
        // Example code:
        /*
        List<Coupon> expiredCoupons = couponRepository.findByExpirationDateBeforeAndIsUsedFalse(now);
        for (Coupon coupon : expiredCoupons) {
            // Store state before save
            couponMongoListener.storeStateBeforeSave(coupon);

            // Mark as used to prevent usage
            coupon.setUsed(true);

            // Save changes
            couponRepository.save(coupon);

            // Publish expired event
            kafkaService.publishCouponExpired(coupon);

            log.info("Marked coupon {} as expired", coupon.getCode());
        }
        */
    }
}