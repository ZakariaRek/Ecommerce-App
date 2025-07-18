package com.Ecommerce.Order_Service.Payload.Kafka.Request;

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
public class CouponValidationRequest {
    private String correlationId;
    private UUID userId;
    private List<String> couponCodes;
    private BigDecimal amount;
    private UUID orderId;
}