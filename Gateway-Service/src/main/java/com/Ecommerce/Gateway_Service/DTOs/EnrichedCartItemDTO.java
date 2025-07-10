package com.Ecommerce.Gateway_Service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedCartItemDTO {
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private LocalDateTime addedAt;

    // Enriched product data
    private String productName;
    private String productImage;
    private Boolean inStock;
    private Integer availableQuantity;
    private String productStatus;
}