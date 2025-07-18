package com.Ecommerce.Loyalty_Service.Payload.Kafka;


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
public class OrderItemDto {
    private UUID productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
}