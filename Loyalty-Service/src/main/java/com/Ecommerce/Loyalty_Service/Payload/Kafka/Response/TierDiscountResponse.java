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
    private UUID userId;
    private String tier;
    private BigDecimal discountAmount;
    private BigDecimal applicableAmount;
    private BigDecimal maxDiscountAmount;
    private BigDecimal discountPercentage;
    private String error;
}