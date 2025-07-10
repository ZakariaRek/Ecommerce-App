package com.Ecommerce.Gateway_Service.DTOs;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductBatchInfoDTO {
    private UUID id;
    private String name;
    private BigDecimal price;
    private String imagePath;
    private Boolean inStock;
    private Integer availableQuantity;
    private ProductStatus status;

    public enum ProductStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK, DISCONTINUED
    }
}