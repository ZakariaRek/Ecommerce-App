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
    private BigDecimal totalDiscount;
    private List<CouponDiscountDetail> validCoupons;
    private List<String> errors;
    private boolean success;
}