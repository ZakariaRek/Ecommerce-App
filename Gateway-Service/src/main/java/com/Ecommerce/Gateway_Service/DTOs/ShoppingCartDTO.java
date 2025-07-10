package com.Ecommerce.Gateway_Service.DTOs;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ShoppingCartDTO {
    private UUID id;
    private UUID userId;
    private List<CartItemDTO> items;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
}