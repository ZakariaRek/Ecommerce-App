package com.Ecommerce.Order_Service.Payload.Kafka.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountBreakdown {
    private String discountType;
    private String description;
    private BigDecimal amount;
    private String source;
}
