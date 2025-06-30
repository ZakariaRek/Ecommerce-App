package com.Ecommerce.Product_Service.Payload.Categorie;


import com.Ecommerce.Product_Service.Payload.Product.ProductSummaryDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CategoryResponseDTO {
    private UUID id;
    private String name;
    private UUID parentId;
    private String parentName; // Resolved from parentId
    private String description;
    private String imageUrl;
    private Integer level;
    private LocalDateTime createdAt;
    private String fullPath; // Calculated path like "Electronics > Phones"
    private Integer productCount;
    private List<ProductSummaryDTO> products;
    private List<CategorySummaryDTO> subcategories; // Categories that have this as parent
}

