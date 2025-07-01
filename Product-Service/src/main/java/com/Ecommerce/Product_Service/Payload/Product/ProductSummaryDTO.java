package com.Ecommerce.Product_Service.Payload.Product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;



@Data
public class ProductSummaryDTO {
    private UUID id;
    private String name;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private Boolean isActive;
    private List<String> supplierNames; // Added for multiple suppliers
}