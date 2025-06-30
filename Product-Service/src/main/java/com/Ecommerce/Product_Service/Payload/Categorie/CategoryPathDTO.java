package com.Ecommerce.Product_Service.Payload.Categorie;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CategoryPathDTO {
    private UUID categoryId;
    private String fullPath;
    private List<CategoryBreadcrumbDTO> breadcrumbs;
}
