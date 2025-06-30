package com.Ecommerce.Product_Service.Utlis;

// CategoryValidator.java - Custom validation for categories


import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Services.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class CategoryValidator {

    @Autowired
    private CategoryService categoryService;

    /**
     * Validate category for creation
     */
    public List<String> validateForCreation(Category category) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            errors.add("Category name is required");
        }

        if (category.getName() != null && category.getName().length() > 100) {
            errors.add("Category name cannot exceed 100 characters");
        }

        // Parent validation
        if (category.getParentId() != null) {
            if (category.getParentId().equals(category.getId())) {
                errors.add("Category cannot be its own parent");
            }

            // Check if parent exists
            if (categoryService.findCategoryById(category.getParentId()).isEmpty()) {
                errors.add("Parent category does not exist");
            }

            // Check maximum depth (e.g., limit to 5 levels)
            int parentLevel = CategoryUtils.calculateLevel(category.getParentId(), categoryService);
            if (parentLevel >= 4) { // 0-based, so 4 means 5 levels deep
                errors.add("Maximum category depth exceeded (5 levels maximum)");
            }
        }

        // Level validation
        if (category.getLevel() != null && category.getLevel() < 0) {
            errors.add("Level cannot be negative");
        }

        return errors;
    }

    /**
     * Validate category for update
     */


    /**
     * Validate category for deletion
     */
    public List<String> validateForDeletion(Category category) {
        List<String> errors = new ArrayList<>();

        // Check if category has subcategories
        List<Category> subcategories = categoryService.findSubcategories(category.getId());
        if (!subcategories.isEmpty()) {
            errors.add("Cannot delete category with subcategories. Please move or delete subcategories first.");
        }

        // Check if category has products
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            errors.add("Cannot delete category with products. Please move products to another category first.");
        }

        return errors;
    }
}