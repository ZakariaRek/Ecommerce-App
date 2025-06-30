package com.Ecommerce.Product_Service.Payload.Categorie;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryStatsDTO {
    private long totalCategories;
    private long rootCategories;
    private int maxDepth;
    private long totalProducts;
    private double averageProductsPerCategory;
    private String mostPopularCategory;
    private LocalDateTime lastUpdated;

    public CategoryStatsDTO() {
        this.lastUpdated = LocalDateTime.now();
    }
}