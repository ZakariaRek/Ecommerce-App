package com.Ecommerce.Order_Service.Payload.Kafka.Response;
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
public class CouponValidationResponse {
    private String correlationId;
    private UUID userId;
    private UUID orderId;
    private BigDecimal totalDiscount;
    private List<CouponDiscountDetail> validCoupons;

    // INCLUDE CONTEXT FOR NEXT STEP
    private BigDecimal originalAmount;
    private BigDecimal productDiscount;
    private BigDecimal orderLevelDiscount;
    private BigDecimal amountAfterCoupons; // For tier calculation

    private boolean success;
    private List<String> errors;
}