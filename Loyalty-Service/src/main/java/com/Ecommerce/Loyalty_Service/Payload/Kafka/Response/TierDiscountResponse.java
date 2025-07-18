package com.Ecommerce.Loyalty_Service.Payload.Kafka.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierDiscountResponse {
    private String correlationId;
    private UUID orderId;
    private UUID userId;
    private String tier;
    private BigDecimal discountAmount;
    private BigDecimal applicableAmount;
    private BigDecimal maxDiscountAmount;
    private BigDecimal discountPercentage;
    private String error;

    // INCLUDE ALL CONTEXT FOR FINAL CALCULATION
    private BigDecimal originalAmount;
    private BigDecimal productDiscount;
    private BigDecimal orderLevelDiscount;
    private BigDecimal couponDiscount;
    private BigDecimal tierDiscount;
    private BigDecimal finalAmount; // Pre-calculated

    private boolean success;
}