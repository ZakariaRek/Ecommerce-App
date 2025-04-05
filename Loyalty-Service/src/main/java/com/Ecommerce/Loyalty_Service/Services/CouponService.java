package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import com.Ecommerce.Loyalty_Service.Repositories.CouponRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class CouponService {
    @Autowired
    private CouponRepository couponRepository;

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

        return couponRepository.save(coupon);
    }

    public boolean validateCoupon(String couponCode, BigDecimal purchaseAmount) {
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        // Check if coupon is valid
        if (coupon.isUsed()) {
            return false;
        }

        if (coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (purchaseAmount.compareTo(coupon.getMinPurchaseAmount()) < 0) {
            return false;
        }

        return true;
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

        // Mark coupon as used
        coupon.setUsed(true);
        couponRepository.save(coupon);

        return purchaseAmount.subtract(discountAmount);
    }

    public List<Coupon> getUserActiveCoupons(UUID userId) {
        return couponRepository.findByUserIdAndIsUsedFalse(userId);
    }
}

