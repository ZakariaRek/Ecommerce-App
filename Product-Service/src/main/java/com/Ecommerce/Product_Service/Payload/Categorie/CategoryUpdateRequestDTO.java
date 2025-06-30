package com.Ecommerce.Product_Service.Payload.Categorie;


import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryUpdateRequestDTO {

    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    private UUID parentId;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 200, message = "Image URL cannot exceed 200 characters")
    private String imageUrl;

    @Min(value = 0, message = "Level cannot be negative")
    private Integer level;
}