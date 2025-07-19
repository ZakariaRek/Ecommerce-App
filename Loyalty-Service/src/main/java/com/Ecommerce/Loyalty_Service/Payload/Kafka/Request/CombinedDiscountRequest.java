// Loyalty-Service: Combined Discount Request DTO
package com.Ecommerce.Loyalty_Service.Payload.Kafka.Request;

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
public class CombinedDiscountRequest {
    private String correlationId;
    private UUID userId;
    private UUID orderId;

    // Amount calculations
    private BigDecimal originalAmount;
    private BigDecimal productDiscount;
    private BigDecimal orderLevelDiscount;
    private BigDecimal amountAfterOrderDiscounts; // Amount to apply coupons and tier discounts to

    // Coupon information
    private List<String> couponCodes;

    // Additional context
    private Integer totalItems;
    private String customerTier; // Optional: if you want to hint the tier
}

// Loyalty-Service: Combined Discount Response DTO
