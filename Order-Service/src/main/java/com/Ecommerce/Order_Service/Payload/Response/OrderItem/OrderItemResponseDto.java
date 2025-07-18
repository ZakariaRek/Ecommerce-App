package com.Ecommerce.Order_Service.Payload.Response.OrderItem;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
public class OrderItemResponseDto {
    private UUID id;
    private UUID productId;
    private int quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal discount;
    private BigDecimal total;
}