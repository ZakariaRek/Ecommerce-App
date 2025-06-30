package com.Ecommerce.Product_Service.Payload.Categorie;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CategorySummaryDTO {
    private UUID id;
    private String name;
    private UUID parentId;
    private String parentName;
    private String description;
    private String imageUrl;
    private Integer level;
    private LocalDateTime createdAt;
    private Integer productCount;
    private Integer subcategoryCount;
}