package com.Ecommerce.Loyalty_Service.Payload.Kafka.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDiscountDetail {
    private String couponCode;

    // FIX: Use String instead of enum for cross-service communication
    // This avoids Jackson deserialization issues between different enum definitions
    private String discountType;

    private BigDecimal discountValue;
    private BigDecimal calculatedDiscount;
}