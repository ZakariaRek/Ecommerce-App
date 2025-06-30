package com.Ecommerce.Product_Service.Payload.Categorie;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CategoryTreeDTO {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer level;
    private Integer productCount;
    private List<CategoryTreeDTO> children;
}
