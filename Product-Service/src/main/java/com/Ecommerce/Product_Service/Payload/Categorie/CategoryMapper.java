// CategoryMapper.java
package com.Ecommerce.Product_Service.Payload.Categorie;

import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Product;

import com.Ecommerce.Product_Service.Payload.Product.ProductSummaryDTO;
import com.Ecommerce.Product_Service.Services.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    @Autowired
    @Lazy // Prevent circular dependency
    private CategoryService categoryService;
    /**
     * Convert CategoryRequestDTO to Category entity
     */
    public Category toEntity(CategoryRequestDTO dto) {
        if (dto == null) return null;

        Category category = new Category();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setDescription(dto.getDescription());
        category.setImageUrl(dto.getImageUrl());
        category.setLevel(dto.getLevel());

        return category;
    }

    /**
     * Convert Category entity to CategoryResponseDTO
     */
    public CategoryResponseDTO toResponseDTO(Category entity) {
        return toResponseDTO(entity, null);
    }

    /**
     * Convert Category entity to CategoryResponseDTO with category cache
     */
    public CategoryResponseDTO toResponseDTO(Category entity, Map<UUID, Category> categoryCache) {
        if (entity == null) return null;

        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setParentId(entity.getParentId());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setLevel(entity.getLevel());
        dto.setCreatedAt(entity.getCreatedAt());

        // Resolve parent name
        if (entity.getParentId() != null) {
            String parentName = resolveParentName(entity.getParentId(), categoryCache);
            dto.setParentName(parentName);
        }

        // Build full path
        dto.setFullPath(buildFullPath(entity, categoryCache));

        // Map products
        if (entity.getProducts() != null) {
            List<ProductSummaryDTO> productSummaries = entity.getProducts().stream()
                    .map(this::toProductSummaryDTO)
                    .collect(Collectors.toList());
            dto.setProducts(productSummaries);
            dto.setProductCount(productSummaries.size());
        } else {
            dto.setProductCount(0);
        }

        // Get subcategories (categories that have this category as parent)
        List<CategorySummaryDTO> subcategories = getSubcategories(entity.getId(), categoryCache);
        dto.setSubcategories(subcategories);

        return dto;
    }

    /**
     * Convert Category entity to CategorySummaryDTO
     */
    public CategorySummaryDTO toSummaryDTO(Category entity) {
        return toSummaryDTO(entity, null);
    }

    /**
     * Convert Category entity to CategorySummaryDTO with category cache
     */
    public CategorySummaryDTO toSummaryDTO(Category entity, Map<UUID, Category> categoryCache) {
        if (entity == null) return null;

        CategorySummaryDTO dto = new CategorySummaryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setParentId(entity.getParentId());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setLevel(entity.getLevel());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setProductCount(entity.getProducts() != null ? entity.getProducts().size() : 0);

        // Resolve parent name
        if (entity.getParentId() != null) {
            String parentName = resolveParentName(entity.getParentId(), categoryCache);
            dto.setParentName(parentName);
        }

        // Count subcategories
        int subcategoryCount = countSubcategories(entity.getId(), categoryCache);
        dto.setSubcategoryCount(subcategoryCount);

        return dto;
    }

    /**
     * Convert Category entity to CategoryTreeDTO (for hierarchical tree view)
     */
    public CategoryTreeDTO toTreeDTO(Category entity, Map<UUID, List<Category>> subcategoryMap) {
        if (entity == null) return null;

        CategoryTreeDTO dto = new CategoryTreeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setLevel(entity.getLevel());
        dto.setProductCount(entity.getProducts() != null ? entity.getProducts().size() : 0);

        // Add children recursively
        List<Category> children = subcategoryMap.get(entity.getId());
        if (children != null && !children.isEmpty()) {
            List<CategoryTreeDTO> childrenDTOs = children.stream()
                    .map(child -> toTreeDTO(child, subcategoryMap))
                    .collect(Collectors.toList());
            dto.setChildren(childrenDTOs);
        } else {
            dto.setChildren(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Convert Category entity to CategoryPathDTO
     */
    public CategoryPathDTO toPathDTO(Category entity) {
        return toPathDTO(entity, null);
    }

    /**
     * Convert Category entity to CategoryPathDTO with category cache
     */
    public CategoryPathDTO toPathDTO(Category entity, Map<UUID, Category> categoryCache) {
        if (entity == null) return null;

        CategoryPathDTO dto = new CategoryPathDTO();
        dto.setCategoryId(entity.getId());
        dto.setFullPath(buildFullPath(entity, categoryCache));
        dto.setBreadcrumbs(buildBreadcrumbs(entity, categoryCache));

        return dto;
    }

    /**
     * Convert Product entity to ProductSummaryDTO
     */
    public ProductSummaryDTO toProductSummaryDTO(Product entity) {
        if (entity == null) return null;

        ProductSummaryDTO dto = new ProductSummaryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSku(entity.getSku());
        dto.setPrice(entity.getPrice());
        dto.setStockQuantity(entity.getStock());

        // Get primary category name
        if (entity.getCategories() != null && !entity.getCategories().isEmpty()) {
            dto.setCategory(entity.getCategories().get(0).getName());
        }


        return dto;
    }

    /**
     * Update existing Category entity from CategoryRequestDTO
     */
    public void updateEntityFromDTO(Category entity, CategoryRequestDTO dto) {
        if (entity == null || dto == null) return;

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getParentId() != null) {
            entity.setParentId(dto.getParentId());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getImageUrl() != null) {
            entity.setImageUrl(dto.getImageUrl());
        }
        if (dto.getLevel() != null) {
            entity.setLevel(dto.getLevel());
        }
    }

    /**
     * Update existing Category entity from CategoryUpdateRequestDTO
     */
    public void updateEntityFromUpdateDTO(Category entity, CategoryUpdateRequestDTO dto) {
        if (entity == null || dto == null) return;

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getParentId() != null) {
            entity.setParentId(dto.getParentId());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getImageUrl() != null) {
            entity.setImageUrl(dto.getImageUrl());
        }
        if (dto.getLevel() != null) {
            entity.setLevel(dto.getLevel());
        }
    }

    /**
     * Convert list of Category entities to list of CategorySummaryDTOs
     */
    public List<CategorySummaryDTO> toSummaryDTOList(List<Category> entities) {
        if (entities == null) return new ArrayList<>();

        // Create category cache for efficient lookups
        Map<UUID, Category> categoryCache = entities.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        return entities.stream()
                .map(entity -> toSummaryDTO(entity, categoryCache))
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Category entities to list of CategoryResponseDTOs
     */
    public List<CategoryResponseDTO> toResponseDTOList(List<Category> entities) {
        if (entities == null) return new ArrayList<>();

        // Create category cache for efficient lookups
        Map<UUID, Category> categoryCache = entities.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        return entities.stream()
                .map(entity -> toResponseDTO(entity, categoryCache))
                .collect(Collectors.toList());
    }

    /**
     * Create category tree from list of all categories
     */
    public List<CategoryTreeDTO> createCategoryTree(List<Category> allCategories) {
        // Group categories by parent ID
        Map<UUID, List<Category>> subcategoryMap = allCategories.stream()
                .filter(cat -> cat.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        // Get root categories (those without parent)
        List<Category> rootCategories = allCategories.stream()
                .filter(cat -> cat.getParentId() == null)
                .collect(Collectors.toList());

        return rootCategories.stream()
                .map(root -> toTreeDTO(root, subcategoryMap))
                .collect(Collectors.toList());
    }

    /**
     * Resolve parent category name
     */
    private String resolveParentName(UUID parentId, Map<UUID, Category> categoryCache) {
        if (parentId == null) return null;

        if (categoryCache != null && categoryCache.containsKey(parentId)) {
            return categoryCache.get(parentId).getName();
        }

        // Fallback to service call if cache not available
        try {
            return categoryService.findCategoryById(parentId)
                    .map(Category::getName)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build full path string for a category
     */
    private String buildFullPath(Category category, Map<UUID, Category> categoryCache) {
        if (category == null) return "";

        List<String> pathParts = new ArrayList<>();
        Category current = category;

        // Avoid infinite loops with a max depth check
        int maxDepth = 10;
        int currentDepth = 0;

        while (current != null && currentDepth < maxDepth) {
            pathParts.add(0, current.getName());

            if (current.getParentId() != null) {
                if (categoryCache != null && categoryCache.containsKey(current.getParentId())) {
                    current = categoryCache.get(current.getParentId());
                } else {
                    // Fallback to service call
                    try {
                        current = categoryService.findCategoryById(current.getParentId()).orElse(null);
                    } catch (Exception e) {
                        break;
                    }
                }
            } else {
                current = null;
            }
            currentDepth++;
        }

        return String.join(" > ", pathParts);
    }

    /**
     * Build breadcrumb list for a category
     */
    private List<CategoryBreadcrumbDTO> buildBreadcrumbs(Category category, Map<UUID, Category> categoryCache) {
        List<CategoryBreadcrumbDTO> breadcrumbs = new ArrayList<>();

        if (category == null) return breadcrumbs;

        Category current = category;
        int maxDepth = 10;
        int currentDepth = 0;

        while (current != null && currentDepth < maxDepth) {
            CategoryBreadcrumbDTO breadcrumb = new CategoryBreadcrumbDTO(
                    current.getId(),
                    current.getName(),
                    current.getLevel()
            );
            breadcrumbs.add(0, breadcrumb);

            if (current.getParentId() != null) {
                if (categoryCache != null && categoryCache.containsKey(current.getParentId())) {
                    current = categoryCache.get(current.getParentId());
                } else {
                    try {
                        current = categoryService.findCategoryById(current.getParentId()).orElse(null);
                    } catch (Exception e) {
                        break;
                    }
                }
            } else {
                current = null;
            }
            currentDepth++;
        }

        return breadcrumbs;
    }

    /**
     * Get subcategories for a given category ID
     */
    private List<CategorySummaryDTO> getSubcategories(UUID categoryId, Map<UUID, Category> categoryCache) {
        if (categoryCache == null) {
            try {
                return categoryService.findSubcategories(categoryId)
                        .stream()
                        .map(this::toSummaryDTO)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        return categoryCache.values().stream()
                .filter(cat -> categoryId.equals(cat.getParentId()))
                .map(cat -> toSummaryDTO(cat, categoryCache))
                .collect(Collectors.toList());
    }

    /**
     * Count subcategories for a given category ID
     */
    private int countSubcategories(UUID categoryId, Map<UUID, Category> categoryCache) {
        if (categoryCache == null) {
            try {
                return categoryService.findSubcategories(categoryId).size();
            } catch (Exception e) {
                return 0;
            }
        }

        return (int) categoryCache.values().stream()
                .filter(cat -> categoryId.equals(cat.getParentId()))
                .count();
    }
}