package com.Ecommerce.Product_Service.Payload.Product;

import com.Ecommerce.Product_Service.Entities.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductRequestDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String sku;
    private BigDecimal weight;
    private String dimensions;
    private List<String> images;
    private ProductStatus status;
    private List<UUID> categoryIds;
    private List<UUID> supplierIds;
}
