// Fixed Loyalty-Service: Combined Discount Service
package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.Entities.*;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Request.CombinedDiscountRequest;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CombinedDiscountResponse;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CouponDiscountDetail;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.DiscountBreakdown;
import com.Ecommerce.Loyalty_Service.Repositories.TierBenefitRepository;
import com.Ecommerce.Loyalty_Service.Services.CRMService;
import com.Ecommerce.Loyalty_Service.Services.CouponValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CombinedDiscountService {

    private final CRMService crmService;
    private final CouponValidationService couponValidationService;
    private final TierBenefitRepository tierBenefitRepository;

    /**
     * Calculate both coupon and tier discounts in a single operation
     */
    public CombinedDiscountResponse calculateCombinedDiscounts(CombinedDiscountRequest request) {
        log.info("💎 LOYALTY SERVICE: Processing combined discount request for user {} with amount {}",
                request.getUserId(), request.getAmountAfterOrderDiscounts());

        try {
            // Step 1: Validate and calculate coupon discounts
            CouponDiscountResult couponResult = processCouponDiscounts(request);

            // Step 2: Calculate amount after coupon discounts
            BigDecimal amountAfterCoupons = request.getAmountAfterOrderDiscounts()
                    .subtract(couponResult.getTotalDiscount());

            // Step 3: Calculate tier discounts on the remaining amount
            TierDiscountResult tierResult = processTierDiscount(request.getUserId(), amountAfterCoupons);

            // Step 4: Build final response
            return buildCombinedResponse(request, couponResult, tierResult, amountAfterCoupons);

        } catch (Exception e) {
            log.error("💎 LOYALTY SERVICE: Error processing combined discounts for user {}: {}",
                    request.getUserId(), e.getMessage());

            return CombinedDiscountResponse.builder()
                    .correlationId(request.getCorrelationId())
                    .userId(request.getUserId())
                    .orderId(request.getOrderId())
                    .success(false)
                    .errorMessage("Failed to calculate combined discounts: " + e.getMessage())
                    .couponDiscount(BigDecimal.ZERO)
                    .tierDiscount(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .finalAmount(request.getAmountAfterOrderDiscounts())
                    .build();
        }
    }

    /**
     * Process coupon discounts (if any coupons provided)
     */
    private CouponDiscountResult processCouponDiscounts(CombinedDiscountRequest request) {
        if (request.getCouponCodes() == null || request.getCouponCodes().isEmpty()) {
            log.info("💎 LOYALTY SERVICE: No coupon codes provided, skipping coupon validation");
            return CouponDiscountResult.empty();
        }

        log.info("💎 LOYALTY SERVICE: Validating coupons: {}", request.getCouponCodes());

        var couponResponse = couponValidationService.validateAndCalculateDiscount(
                request.getCouponCodes(),
                request.getUserId(),
                request.getAmountAfterOrderDiscounts()
        );

        // FIX: Map coupon discount details to Order Service expected format
        List<CouponDiscountDetail> mappedCoupons = null;
        if (couponResponse.getValidCoupons() != null) {
            mappedCoupons = couponResponse.getValidCoupons().stream()
                    .map(this::mapCouponDiscountDetail)
                    .collect(Collectors.toList());
        }

        return CouponDiscountResult.builder()
                .totalDiscount(couponResponse.getTotalDiscount())
                .validCoupons(mappedCoupons)
                .errors(couponResponse.getErrors())
                .success(couponResponse.isSuccess())
                .build();
    }

    /**
     * FIX: Map internal CouponDiscountDetail to Order Service expected format
     */
    private CouponDiscountDetail mapCouponDiscountDetail(CouponDiscountDetail internal) {
        return CouponDiscountDetail.builder()
                .couponCode(internal.getCouponCode())
                .discountType(mapToOrderServiceDiscountType(DiscountType.valueOf(internal.getDiscountType()))) // Returns String
                .discountValue(internal.getDiscountValue())
                .calculatedDiscount(internal.getCalculatedDiscount())
                .build();
    }

    /**
     * FIX: Map Loyalty Service DiscountType to Order Service DiscountType STRING
     */
    private String mapToOrderServiceDiscountType(com.Ecommerce.Loyalty_Service.Entities.DiscountType loyaltyType) {
        if (loyaltyType == null) {
            return "LOYALTY_COUPON"; // Default fallback as string
        }

        // Map from Loyalty Service enum to Order Service enum VALUES AS STRINGS
        switch (loyaltyType) {
            case PERCENTAGE:
            case FIXED_AMOUNT:
                return "LOYALTY_COUPON";
            default:
                log.warn("💎 LOYALTY SERVICE: Unknown discount type: {}, defaulting to LOYALTY_COUPON", loyaltyType);
                return "LOYALTY_COUPON";
        }
    }

    /**
     * Process tier discount
     */
    private TierDiscountResult processTierDiscount(UUID userId, BigDecimal amount) {
        try {
            log.info("💎 LOYALTY SERVICE: Calculating tier discount for user {} with amount {}", userId, amount);

            CRM crm = crmService.getByUserId(userId);
            MembershipTier tier = crm.getMembershipLevel();

            log.info("💎 LOYALTY SERVICE: User {} has tier: {}", userId, tier);

            var benefitOpt = tierBenefitRepository
                    .findByTierAndBenefitTypeAndActiveTrue(tier, BenefitType.DISCOUNT);

            if (benefitOpt.isEmpty()) {
                log.info("💎 LOYALTY SERVICE: No discount benefit found for tier: {}", tier);
                return TierDiscountResult.empty(tier.toString());
            }

            TierBenefit benefit = benefitOpt.get();

            // Check minimum order amount
            if (benefit.getMinOrderAmount() != null &&
                    amount.compareTo(benefit.getMinOrderAmount()) < 0) {
                log.info("💎 LOYALTY SERVICE: Amount {} below minimum {} for tier {}",
                        amount, benefit.getMinOrderAmount(), tier);
                return TierDiscountResult.empty(tier.toString());
            }

            BigDecimal discountAmount = amount
                    .multiply(benefit.getDiscountPercentage().divide(new BigDecimal("100")));

            // Apply max discount limit
            if (benefit.getMaxDiscountAmount() != null &&
                    discountAmount.compareTo(benefit.getMaxDiscountAmount()) > 0) {
                discountAmount = benefit.getMaxDiscountAmount();
                log.info("💎 LOYALTY SERVICE: Discount capped at maximum: {}", discountAmount);
            }

            log.info("💎 LOYALTY SERVICE: Tier {} discount calculated: {}", tier, discountAmount);

            return TierDiscountResult.builder()
                    .discountAmount(discountAmount)
                    .tier(tier.toString())
                    .discountPercentage(benefit.getDiscountPercentage())
                    .maxDiscountAmount(benefit.getMaxDiscountAmount())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("💎 LOYALTY SERVICE: Error calculating tier discount for user {}: {}", userId, e.getMessage());
            return TierDiscountResult.empty("UNKNOWN");
        }
    }

    /**
     * Build the final combined response
     */
    private CombinedDiscountResponse buildCombinedResponse(CombinedDiscountRequest request,
                                                           CouponDiscountResult couponResult,
                                                           TierDiscountResult tierResult,
                                                           BigDecimal amountAfterCoupons) {

        BigDecimal totalLoyaltyDiscount = couponResult.getTotalDiscount().add(tierResult.getDiscountAmount());
        BigDecimal finalAmount = amountAfterCoupons.subtract(tierResult.getDiscountAmount());

        // Create detailed breakdown
        List<DiscountBreakdown> breakdown = createDetailedBreakdown(
                request, couponResult, tierResult);

        return CombinedDiscountResponse.builder()
                .correlationId(request.getCorrelationId())
                .userId(request.getUserId())
                .orderId(request.getOrderId())
                .originalAmount(request.getOriginalAmount())
                .productDiscount(request.getProductDiscount())
                .orderLevelDiscount(request.getOrderLevelDiscount())
                .couponDiscount(couponResult.getTotalDiscount())
                .validCoupons(couponResult.getValidCoupons())
                .couponErrors(couponResult.getErrors())
                .tierDiscount(tierResult.getDiscountAmount())
                .membershipTier(tierResult.getTier())
                .tierDiscountPercentage(tierResult.getDiscountPercentage())
                .maxTierDiscountAmount(tierResult.getMaxDiscountAmount())
                .totalDiscount(totalLoyaltyDiscount)
                .finalAmount(finalAmount)
                .breakdown(breakdown)
                .success(true)
                .couponSuccess(couponResult.isSuccess())
                .tierDiscountSuccess(tierResult.isSuccess())
                .build();
    }

    /**
     * Create detailed breakdown of all discounts applied
     */
    private List<DiscountBreakdown> createDetailedBreakdown(CombinedDiscountRequest request,
                                                            CouponDiscountResult couponResult,
                                                            TierDiscountResult tierResult) {
        List<DiscountBreakdown> breakdown = new ArrayList<>();

        // Add product discounts if any
        if (request.getProductDiscount().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("PRODUCT")
                    .description("Product-level discounts")
                    .amount(request.getProductDiscount())
                    .source("Product Service")
                    .build());
        }

        // Add order-level discounts if any
        if (request.getOrderLevelDiscount().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("ORDER_LEVEL")
                    .description("Order-level discounts (bulk, minimum purchase)")
                    .amount(request.getOrderLevelDiscount())
                    .source("Order Service")
                    .build());
        }

        // Add coupon discounts if any
        if (couponResult.getTotalDiscount().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("LOYALTY_COUPON")
                    .description("Loyalty coupon discounts (" +
                            (couponResult.getValidCoupons() != null ?
                                    couponResult.getValidCoupons().size() : 0) + " coupons)")
                    .amount(couponResult.getTotalDiscount())
                    .source("Loyalty Service")
                    .build());
        }

        // Add tier discounts if any
        if (tierResult.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("TIER_BENEFIT")
                    .description("Membership tier benefit (" + tierResult.getTier() + ")")
                    .amount(tierResult.getDiscountAmount())
                    .source("Loyalty Service")
                    .build());
        }

        return breakdown;
    }

    // Helper classes for internal processing
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class CouponDiscountResult {
        private BigDecimal totalDiscount;
        private List<CouponDiscountDetail> validCoupons;
        private List<String> errors;
        private boolean success;

        static CouponDiscountResult empty() {
            return CouponDiscountResult.builder()
                    .totalDiscount(BigDecimal.ZERO)
                    .validCoupons(new ArrayList<>())
                    .errors(new ArrayList<>())
                    .success(true)
                    .build();
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class TierDiscountResult {
        private BigDecimal discountAmount;
        private String tier;
        private BigDecimal discountPercentage;
        private BigDecimal maxDiscountAmount;
        private boolean success;

        static TierDiscountResult empty(String tier) {
            return TierDiscountResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .tier(tier)
                    .success(true)
                    .build();
        }
    }
}