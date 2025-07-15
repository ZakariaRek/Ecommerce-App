package com.Ecommerce.Gateway_Service.DTOs.Saved4Later;

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
public class EnrichedSavedItemDTO {
    // Saved item fields
    private UUID id;
    private UUID userId;
    private UUID productId;
    private LocalDateTime savedAt;

    // Enriched product data
    private String productName;
    private String productImage;
    private Boolean inStock;
    private Integer availableQuantity;
    private String productStatus;
    private BigDecimal price;
    private BigDecimal discountValue;
    private String discountType;


}
