package com.Ecommerce.Order_Service.Payload.Response.OrderItem;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemResponseDto {
    private UUID id;
    private UUID productId;
    private int quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal discount;
    private BigDecimal total;
}