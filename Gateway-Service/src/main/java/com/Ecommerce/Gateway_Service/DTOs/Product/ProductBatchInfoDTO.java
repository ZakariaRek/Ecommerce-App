package com.Ecommerce.Gateway_Service.DTOs.Product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;
@Builder
@Data
public class ProductBatchInfoDTO {
    private UUID id;
    private String name;
    private String imagePath;
    private Boolean inStock;
    private Integer availableQuantity;
    private String status;
    private BigDecimal price;
    private Status productStatus;
    private BigDecimal discountValue;
    private String discountType;

    public enum Status {
        AVAILABLE,
        UNAVAILABLE,
        DISCONTINUED
    }


}