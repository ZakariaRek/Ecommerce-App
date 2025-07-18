package com.Ecommerce.Order_Service.Payload.Kafka.Response;


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
public class DiscountCalculationResponse {
    private String correlationId;
    private UUID orderId;
    private BigDecimal originalAmount;
    private BigDecimal productDiscount;
    private BigDecimal orderLevelDiscount;
    private BigDecimal couponDiscount;
    private BigDecimal tierDiscount;
    private BigDecimal finalAmount;
    private List<DiscountBreakdown> breakdown;
    private boolean success;
    private String errorMessage;
}
