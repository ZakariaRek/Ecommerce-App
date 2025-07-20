package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CouponDiscountDetail;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CouponValidationResponse;
import com.Ecommerce.Loyalty_Service.Repositories.CouponRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponValidationService {

    private  final CouponRepository couponRepository;

    public CouponValidationResponse validateAndCalculateDiscount(
            List<String> couponCodes, UUID userId, BigDecimal amount) {

        log.info("💳 LOYALTY SERVICE: Validating coupons {} for user {} with amount {}",
                couponCodes, userId, amount);

        List<CouponDiscountDetail> validCoupons = new ArrayList<>();
        BigDecimal totalCouponDiscount = BigDecimal.ZERO;
        List<String> errors = new ArrayList<>();

        for (String couponCode : couponCodes) {
            try {
                Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);

                if (couponOpt.isEmpty()) {
                    errors.add("Coupon not found: " + couponCode);
                    log.warn("💳 LOYALTY SERVICE: Coupon not found: {}", couponCode);
                    continue;
                }

                Coupon coupon = couponOpt.get();

                // Validate coupon
                ValidationResult validation = validateSingleCoupon(coupon, userId, amount);
                if (!validation.isValid()) {
                    errors.add("Invalid coupon " + couponCode + ": " + validation.getError());
                    log.warn("💳 LOYALTY SERVICE: Invalid coupon {}: {}", couponCode, validation.getError());
                    continue;
                }

                // Calculate discount
                BigDecimal discount = calculateCouponDiscount(coupon, amount);
                totalCouponDiscount = totalCouponDiscount.add(discount);

                validCoupons.add(CouponDiscountDetail.builder()
                        .couponCode(couponCode)
                        .discountType(String.valueOf(coupon.getDiscountType()))
                        .discountValue(coupon.getDiscountValue())
                        .calculatedDiscount(discount)
                        .build());

                log.info("💳 LOYALTY SERVICE: Valid coupon {}: discount {}", couponCode, discount);

            } catch (Exception e) {
                errors.add("Error processing coupon " + couponCode + ": " + e.getMessage());
                log.error("💳 LOYALTY SERVICE: Error processing coupon {}: {}", couponCode, e.getMessage());
            }
        }

        boolean success = errors.isEmpty() && !validCoupons.isEmpty();

        log.info("💳 LOYALTY SERVICE: Coupon validation complete. Success: {}, Total discount: {}",
                success, totalCouponDiscount);

        return CouponValidationResponse.builder()
                .userId(userId)
                .totalDiscount(totalCouponDiscount)
                .validCoupons(validCoupons)
                .errors(errors)
                .success(success)
                .build();
    }

    private ValidationResult validateSingleCoupon(Coupon coupon, UUID userId, BigDecimal amount) {
        // Check if expired
        if (coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            return ValidationResult.invalid("Coupon has expired");
        }

        // Check if already used (for single-use coupons)
        if (coupon.isUsed()) {
            return ValidationResult.invalid("Coupon has already been used");
        }

        // Check minimum purchase amount
        if (amount.compareTo(coupon.getMinPurchaseAmount()) < 0) {
            return ValidationResult.invalid("Minimum purchase amount of " +
                    coupon.getMinPurchaseAmount() + " not met");
        }



        return ValidationResult.valid();
    }

    private BigDecimal calculateCouponDiscount(Coupon coupon, BigDecimal amount) {
        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = amount.multiply(coupon.getDiscountValue().divide(new BigDecimal("100")));
        } else {
            discount = coupon.getDiscountValue();
        }

        // Apply maximum discount constraint
        if (coupon.getMaxDiscountAmount() != null &&
                discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }

        return discount;
    }

    @Data
    @AllArgsConstructor
    private static class ValidationResult {
        private boolean valid;
        private String error;

        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }
    }
}