package com.Ecommerce.Loyalty_Service.Payload.Kafka.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinedDiscountResponse {
    private String correlationId;
    private UUID userId;
    private UUID orderId;

    // Original amounts
    private BigDecimal originalAmount;
    private BigDecimal productDiscount;
    private BigDecimal orderLevelDiscount;

    // Coupon discount details
    private BigDecimal couponDiscount;
    private List<CouponDiscountDetail> validCoupons;
    private List<String> couponErrors;

    // Tier discount details
    private BigDecimal tierDiscount;
    private String membershipTier;
    private BigDecimal tierDiscountPercentage;
    private BigDecimal maxTierDiscountAmount;

    // Final calculation
    private BigDecimal totalDiscount; // couponDiscount + tierDiscount
    private BigDecimal finalAmount;

    // Detailed breakdown
    private List<DiscountBreakdown> breakdown;

    // Status
    private boolean success;
    private String errorMessage;
    private boolean couponSuccess;
    private boolean tierDiscountSuccess;
}
