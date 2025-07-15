package com.Ecommerce.Gateway_Service.DTOs.Cart;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CartItemDTO {
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private LocalDateTime addedAt;
}