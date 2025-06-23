package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Services.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAllCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable UUID id) {
        return categoryService.findCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/root")
    public ResponseEntity<List<Category>> getRootCategories() {
        return ResponseEntity.ok(categoryService.findRootCategories());
    }

    @GetMapping("/subcategories/{parentId}")
    public ResponseEntity<List<Category>> getSubcategories(@PathVariable UUID parentId) {
        return ResponseEntity.ok(categoryService.findSubcategories(parentId));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<Category>> getCategoriesByLevel(@PathVariable Integer level) {
        return ResponseEntity.ok(categoryService.findCategoriesByLevel(level));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        try {
            Category createdCategory = categoryService.addCategory(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable UUID id, @RequestBody Category category) {
        try {
            return categoryService.updateCategory(id, category)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<Map<String, String>> getCategoryPath(@PathVariable UUID id) {
        String path = categoryService.getFullPath(id);
        if (path.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("path", path));
    }

    @PostMapping("/{categoryId}/products/{productId}")
    public ResponseEntity<Category> addProductToCategory(
            @PathVariable UUID categoryId,
            @PathVariable UUID productId) {
        return categoryService.addProductToCategory(categoryId, productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{categoryId}/products/{productId}")
    public ResponseEntity<Category> removeProductFromCategory(
            @PathVariable UUID categoryId,
            @PathVariable UUID productId) {
        return categoryService.removeProductFromCategory(categoryId, productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getCategoryTree() {
        return ResponseEntity.ok(categoryService.getCategoryTree());
    }
}