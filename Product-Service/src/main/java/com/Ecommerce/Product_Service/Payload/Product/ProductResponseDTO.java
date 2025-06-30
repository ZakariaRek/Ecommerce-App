package com.Ecommerce.Product_Service.Payload.Product;


import com.Ecommerce.Product_Service.Entities.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProductResponseDTO {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String sku;
    private BigDecimal weight;
    private String dimensions;
    private List<String> images;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}