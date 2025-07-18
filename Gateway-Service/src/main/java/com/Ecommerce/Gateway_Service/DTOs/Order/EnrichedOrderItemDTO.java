package com.Ecommerce.Gateway_Service.DTOs;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class EnrichedOrderItemDTO {

    // Order item fields
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal discount;
    private BigDecimal total;

    // Product details (enriched from product service)
    private String productName;
    private String productImage;
    private String productStatus;
    private Boolean inStock;
    private Integer availableQuantity;
    private String discountType;
    private BigDecimal discountValue;

}