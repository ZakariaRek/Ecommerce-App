package com.Ecommerce.Product_Service.Controllers;


import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Payload.Categorie.*;
import com.Ecommerce.Product_Service.Payload.Product.ProductSummaryDTO;
import com.Ecommerce.Product_Service.Services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
@Validated
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping
    public ResponseEntity<List<CategorySummaryDTO>> getAllCategories() {
        List<CategorySummaryDTO> categories = categoryMapper.toSummaryDTOList(
                categoryService.findAllCategories()
        );

        // Sort by level first, then by name
        categories.sort((c1, c2) -> {
            int levelCompare = Integer.compare(
                    c1.getLevel() != null ? c1.getLevel() : 0,
                    c2.getLevel() != null ? c2.getLevel() : 0
            );
            return levelCompare != 0 ? levelCompare : c1.getName().compareTo(c2.getName());
        });

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable UUID id) {
        return categoryService.findCategoryById(id)
                .map(categoryMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/root")
    public ResponseEntity<List<CategorySummaryDTO>> getRootCategories() {
        List<CategorySummaryDTO> rootCategories = categoryMapper.toSummaryDTOList(
                categoryService.findRootCategories()
        );
        return ResponseEntity.ok(rootCategories);
    }

    @GetMapping("/subcategories/{parentId}")
    public ResponseEntity<List<CategorySummaryDTO>> getSubcategories(@PathVariable UUID parentId) {
        List<CategorySummaryDTO> subcategories = categoryMapper.toSummaryDTOList(
                categoryService.findSubcategories(parentId)
        );
        return ResponseEntity.ok(subcategories);
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<CategorySummaryDTO>> getCategoriesByLevel(@PathVariable Integer level) {
        if (level < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Level cannot be negative");
        }

        List<CategorySummaryDTO> categories = categoryMapper.toSummaryDTOList(
                categoryService.findCategoriesByLevel(level)
        );
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDTO categoryRequest) {
        try {
            var category = categoryMapper.toEntity(categoryRequest);
            var createdCategory = categoryService.addCategory(category);
            var responseDTO = categoryMapper.toResponseDTO(createdCategory);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating category");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequestDTO categoryRequest) {
        try {
            return categoryService.findCategoryById(id)
                    .map(existingCategory -> {
                        categoryMapper.updateEntityFromDTO(existingCategory, categoryRequest);
                        return categoryService.updateCategory(id, existingCategory)
                                .map(categoryMapper::toResponseDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating category");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> partialUpdateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequestDTO updateRequest) {
        try {
            return categoryService.findCategoryById(id)
                    .map(existingCategory -> {
                        categoryMapper.updateEntityFromUpdateDTO(existingCategory, updateRequest);
                        return categoryService.updateCategory(id, existingCategory)
                                .map(categoryMapper::toResponseDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating category");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        try {
            if (categoryService.findCategoryById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting category");
        }
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<CategoryPathDTO> getCategoryPath(@PathVariable UUID id) {
        return categoryService.findCategoryById(id)
                .map(categoryMapper::toPathDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{categoryId}/products/{productId}")
    public ResponseEntity<CategoryResponseDTO> addProductToCategory(
            @PathVariable UUID categoryId,
            @PathVariable UUID productId) {
        try {
            return categoryService.addProductToCategory(categoryId, productId)
                    .map(categoryMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding product to category");
        }
    }

    @DeleteMapping("/{categoryId}/products/{productId}")
    public ResponseEntity<CategoryResponseDTO> removeProductFromCategory(
            @PathVariable UUID categoryId,
            @PathVariable UUID productId) {
        try {
            return categoryService.removeProductFromCategory(categoryId, productId)
                    .map(categoryMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error removing product from category");
        }
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeDTO>> getCategoryTree() {
        List<Category> allCategories = categoryService.findAllCategories();
        List<CategoryTreeDTO> tree = categoryMapper.createCategoryTree(allCategories);
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/{categoryId}/products")
    public ResponseEntity<List<ProductSummaryDTO>> getProductsByCategory(@PathVariable UUID categoryId) {
        try {
            return categoryService.findCategoryById(categoryId)
                    .map(category -> {
                        List<ProductSummaryDTO> products = category.getProducts().stream()
                                .map(categoryMapper::toProductSummaryDTO)
                                .collect(Collectors.toList());
                        return ResponseEntity.ok(products);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving products for category");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategorySummaryDTO>> searchCategories(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Boolean hasProducts) {

        List<CategorySummaryDTO> categories = categoryService.findAllCategories()
                .stream()
                .filter(category -> {
                    boolean matches = true;

                    if (name != null && !name.isEmpty()) {
                        matches = matches && category.getName().toLowerCase().contains(name.toLowerCase());
                    }

                    if (parentId != null) {
                        matches = matches && parentId.equals(category.getParentId());
                    }

                    if (level != null) {
                        matches = matches && level.equals(category.getLevel());
                    }

                    if (hasProducts != null) {
                        boolean categoryHasProducts = category.getProducts() != null && !category.getProducts().isEmpty();
                        matches = matches && hasProducts.equals(categoryHasProducts);
                    }

                    return matches;
                })
                .map(categoryMapper::toSummaryDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/stats")
    public ResponseEntity<CategoryStatsDTO> getCategoryStatistics() {
        try {
            var allCategories = categoryService.findAllCategories();

            CategoryStatsDTO stats = new CategoryStatsDTO();
            stats.setTotalCategories(allCategories.size());

            long rootCount = allCategories.stream()
                    .filter(cat -> cat.getParentId() == null)
                    .count();
            stats.setRootCategories(rootCount);

            // Calculate max depth
            int maxDepth = allCategories.stream()
                    .mapToInt(cat -> cat.getLevel() != null ? cat.getLevel() : 0)
                    .max()
                    .orElse(0);
            stats.setMaxDepth(maxDepth);

            // Calculate total products across all categories
            long totalProducts = allCategories.stream()
                    .mapToLong(cat -> cat.getProducts() != null ? cat.getProducts().size() : 0)
                    .sum();
            stats.setTotalProducts(totalProducts);

            // Calculate average products per category
            if (!allCategories.isEmpty()) {
                double avgProducts = (double) totalProducts / allCategories.size();
                stats.setAverageProductsPerCategory(avgProducts);
            }

            // Find most popular category (one with most products)
            String mostPopular = allCategories.stream()
                    .filter(cat -> cat.getProducts() != null && !cat.getProducts().isEmpty())
                    .max((c1, c2) -> Integer.compare(c1.getProducts().size(), c2.getProducts().size()))
                    .map(cat -> cat.getName())
                    .orElse("None");
            stats.setMostPopularCategory(mostPopular);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving category statistics");
        }
    }

    @PostMapping("/{categoryId}/move")
    public ResponseEntity<CategoryResponseDTO> moveCategory(
            @PathVariable UUID categoryId,
            @RequestParam(required = false) UUID newParentId) {
        try {
            return categoryService.findCategoryById(categoryId)
                    .map(category -> {
                        category.setParentId(newParentId);
                        // Recalculate level based on new parent
                        int newLevel = calculateLevel(newParentId);
                        category.setLevel(newLevel);

                        return categoryService.updateCategory(categoryId, category)
                                .map(categoryMapper::toResponseDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error moving category");
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<CategoryResponseDTO>> createBulkCategories(
            @Valid @RequestBody List<CategoryRequestDTO> categoryRequests) {
        try {
            List<CategoryResponseDTO> createdCategories = categoryRequests.stream()
                    .map(categoryMapper::toEntity)
                    .map(category -> {
                        try {
                            return categoryService.addCategory(category);
                        } catch (Exception e) {
                            System.err.println("Failed to create category: " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(category -> category != null)
                    .map(categoryMapper::toResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategories);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating bulk categories");
        }
    }

    // Helper method to calculate level based on parent
    private int calculateLevel(UUID parentId) {
        if (parentId == null) {
            return 0; // Root level
        }

        return categoryService.findCategoryById(parentId)
                .map(parent -> (parent.getLevel() != null ? parent.getLevel() : 0) + 1)
                .orElse(0);
    }

    // Additional utility endpoints

    @GetMapping("/hierarchy/{categoryId}")
    public ResponseEntity<Map<String, Object>> getCategoryHierarchy(@PathVariable UUID categoryId) {
        try {
            return categoryService.findCategoryById(categoryId)
                    .map(category -> {
                        CategoryPathDTO path = categoryMapper.toPathDTO(category);
                        List<CategorySummaryDTO> subcategories = categoryMapper.toSummaryDTOList(
                                categoryService.findSubcategories(categoryId)
                        );

                        Map<String, Object> hierarchy = Map.of(
                                "category", categoryMapper.toResponseDTO(category),
                                "path", path,
                                "subcategories", subcategories
                        );

                        return ResponseEntity.ok(hierarchy);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving category hierarchy");
        }
    }

    @GetMapping("/by-product/{productId}")
    public ResponseEntity<List<CategorySummaryDTO>> getCategoriesByProduct(@PathVariable UUID productId) {
        try {
            // This would need to be implemented in your service
            // For now, returning empty list as placeholder
            return ResponseEntity.ok(Collections.emptyList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving categories for product");
        }
    }
}