// CategoryService.java - Service with integrated utilities and validation
package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Repositories.CategoryRepository;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    // ====== BASIC CRUD OPERATIONS ======

    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findCategoryById(UUID id) {
        return categoryRepository.findById(id);
    }

    public List<Category> findRootCategories() {
        return categoryRepository.findByParentIdIsNull();
    }

    public List<Category> findSubcategories(UUID parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public List<Category> findCategoriesByLevel(Integer level) {
        return categoryRepository.findByLevel(level);
    }

    public Category addCategory(Category category) {
        // Validate category
        List<String> errors = validateForCreation(category);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + String.join(", ", errors));
        }

        // Auto-calculate level if not provided
        if (category.getLevel() == null) {
            category.setLevel(calculateCategoryLevel(category.getParentId()));
        }

        return categoryRepository.save(category);
    }

    public Optional<Category> updateCategory(UUID id, Category updatedCategory) {
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    // Validate update
                    List<String> errors = validateForUpdate(existingCategory, updatedCategory.getParentId());
                    if (!errors.isEmpty()) {
                        throw new IllegalArgumentException("Validation failed: " + String.join(", ", errors));
                    }

                    // Update fields
                    if (updatedCategory.getName() != null) {
                        existingCategory.setName(updatedCategory.getName());
                    }
                    if (updatedCategory.getDescription() != null) {
                        existingCategory.setDescription(updatedCategory.getDescription());
                    }
                    if (updatedCategory.getImageUrl() != null) {
                        existingCategory.setImageUrl(updatedCategory.getImageUrl());
                    }

                    // Handle parent change
                    if (!Objects.equals(updatedCategory.getParentId(), existingCategory.getParentId())) {
                        existingCategory.setParentId(updatedCategory.getParentId());
                        // Recalculate level when parent changes
                        existingCategory.setLevel(calculateCategoryLevel(updatedCategory.getParentId()));
                    }

                    if (updatedCategory.getLevel() != null) {
                        existingCategory.setLevel(updatedCategory.getLevel());
                    }

                    return categoryRepository.save(existingCategory);
                });
    }

    public void deleteCategory(UUID id) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category not found");
        }

        Category category = categoryOpt.get();

        // Validate deletion
        List<String> errors = validateForDeletion(category);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Cannot delete category: " + String.join(", ", errors));
        }

        categoryRepository.deleteById(id);
    }

    // ====== PRODUCT-CATEGORY OPERATIONS ======

    public Optional<Category> addProductToCategory(UUID categoryId, UUID productId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (categoryOpt.isPresent() && productOpt.isPresent()) {
            Category category = categoryOpt.get();
            Product product = productOpt.get();

            // Add to many-to-many relationship
            if (!category.getProducts().contains(product)) {
                category.getProducts().add(product);
                product.getCategories().add(category);

                productRepository.save(product);
                return Optional.of(categoryRepository.save(category));
            }
            return Optional.of(category);
        }

        return Optional.empty();
    }

    public Optional<Category> removeProductFromCategory(UUID categoryId, UUID productId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (categoryOpt.isPresent() && productOpt.isPresent()) {
            Category category = categoryOpt.get();
            Product product = productOpt.get();

            // Remove from many-to-many relationship
            category.getProducts().removeIf(p -> p.getId().equals(productId));
            product.getCategories().removeIf(c -> c.getId().equals(categoryId));

            productRepository.save(product);
            return Optional.of(categoryRepository.save(category));
        }

        return Optional.empty();
    }

    // ====== UTILITY METHODS ======

    public String getFullPath(UUID categoryId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return "";
        }

        return buildFullPath(categoryOpt.get());
    }

    public Map<String, Object> getCategoryTree() {
        List<Category> allCategories = findAllCategories();

        // Group categories by parent ID
        Map<UUID, List<Category>> subcategoryMap = allCategories.stream()
                .filter(cat -> cat.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        // Get root categories
        List<Category> rootCategories = allCategories.stream()
                .filter(cat -> cat.getParentId() == null)
                .collect(Collectors.toList());

        // Build tree structure
        List<Map<String, Object>> tree = rootCategories.stream()
                .map(root -> buildCategoryNode(root, subcategoryMap))
                .collect(Collectors.toList());

        return Map.of(
                "categories", tree,
                "totalCount", allCategories.size(),
                "rootCount", rootCategories.size()
        );
    }

    public List<Category> findCategoriesWithProducts() {
        return findAllCategories().stream()
                .filter(cat -> cat.getProducts() != null && !cat.getProducts().isEmpty())
                .collect(Collectors.toList());
    }

    public List<Category> findEmptyCategories() {
        return findAllCategories().stream()
                .filter(cat -> cat.getProducts() == null || cat.getProducts().isEmpty())
                .collect(Collectors.toList());
    }

    public boolean hasSubcategories(UUID categoryId) {
        return findSubcategories(categoryId).size() > 0;
    }

    public int calculateCategoryLevel(UUID parentId) {
        if (parentId == null) {
            return 0; // Root level
        }

        return categoryRepository.findById(parentId)
                .map(parent -> (parent.getLevel() != null ? parent.getLevel() : 0) + 1)
                .orElse(0);
    }

    public List<UUID> getCategoryPath(UUID categoryId) {
        List<UUID> path = new ArrayList<>();

        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return path;
        }

        Category current = categoryOpt.get();
        int maxDepth = 10; // Prevent infinite loops
        int currentDepth = 0;

        while (current != null && currentDepth < maxDepth) {
            path.add(0, current.getId()); // Add to beginning

            if (current.getParentId() != null) {
                current = categoryRepository.findById(current.getParentId()).orElse(null);
            } else {
                current = null;
            }
            currentDepth++;
        }

        return path;
    }

    public boolean canMoveCategory(UUID categoryId, UUID newParentId) {
        if (categoryId == null || newParentId == null) {
            return true; // Can always move to root (null parent)
        }

        if (categoryId.equals(newParentId)) {
            return false; // Cannot be its own parent
        }

        // Check if newParent is a descendant of category (would create circular reference)
        return !isDescendant(newParentId, categoryId);
    }

    public List<Category> sortCategories(List<Category> categories) {
        return categories.stream()
                .sorted((c1, c2) -> {
                    int levelCompare = Integer.compare(
                            c1.getLevel() != null ? c1.getLevel() : 0,
                            c2.getLevel() != null ? c2.getLevel() : 0
                    );
                    return levelCompare != 0 ? levelCompare : c1.getName().compareTo(c2.getName());
                })
                .collect(Collectors.toList());
    }

    public Map<UUID, String> buildPathMap(List<Category> categories) {
        Map<UUID, String> pathMap = new HashMap<>();
        Map<UUID, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        for (Category category : categories) {
            String path = buildFullPath(category, categoryMap);
            pathMap.put(category.getId(), path);
        }

        return pathMap;
    }

    public void moveCategory(UUID categoryId, UUID newParentId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category not found");
        }

        Category category = categoryOpt.get();

        // Validate the move
        if (!canMoveCategory(categoryId, newParentId)) {
            throw new IllegalArgumentException("Cannot move category to the specified parent (would create circular reference)");
        }

        if (newParentId != null && categoryRepository.findById(newParentId).isEmpty()) {
            throw new IllegalArgumentException("New parent category not found");
        }

        // Update parent and recalculate level
        category.setParentId(newParentId);
        category.setLevel(calculateCategoryLevel(newParentId));

        // Update levels for all descendants
        updateDescendantLevels(categoryId);

        categoryRepository.save(category);
    }

    // ====== PRIVATE UTILITY METHODS ======

    private String buildFullPath(Category category) {
        Map<UUID, Category> categoryMap = findAllCategories().stream()
                .collect(Collectors.toMap(Category::getId, cat -> cat));
        return buildFullPath(category, categoryMap);
    }

    private String buildFullPath(Category category, Map<UUID, Category> categoryMap) {
        if (category == null) return "";

        List<String> pathParts = new ArrayList<>();
        Category current = category;
        int maxDepth = 10; // Prevent infinite loops
        int currentDepth = 0;

        while (current != null && currentDepth < maxDepth) {
            pathParts.add(0, current.getName());

            if (current.getParentId() != null && categoryMap.containsKey(current.getParentId())) {
                current = categoryMap.get(current.getParentId());
            } else {
                current = null;
            }
            currentDepth++;
        }

        return String.join(" > ", pathParts);
    }

    private Map<String, Object> buildCategoryNode(Category category, Map<UUID, List<Category>> subcategoryMap) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", category.getId());
        node.put("name", category.getName());
        node.put("description", category.getDescription());
        node.put("level", category.getLevel());
        node.put("productCount", category.getProducts() != null ? category.getProducts().size() : 0);

        // Add children recursively
        List<Category> children = subcategoryMap.get(category.getId());
        if (children != null && !children.isEmpty()) {
            List<Map<String, Object>> childNodes = children.stream()
                    .map(child -> buildCategoryNode(child, subcategoryMap))
                    .collect(Collectors.toList());
            node.put("children", childNodes);
        } else {
            node.put("children", new ArrayList<>());
        }

        return node;
    }

    private boolean isDescendant(UUID potentialDescendantId, UUID ancestorId) {
        if (potentialDescendantId == null || ancestorId == null) {
            return false;
        }

        Optional<Category> potentialDescendant = categoryRepository.findById(potentialDescendantId);
        if (potentialDescendant.isEmpty()) {
            return false;
        }

        UUID currentParentId = potentialDescendant.get().getParentId();
        int maxDepth = 10; // Prevent infinite loops
        int currentDepth = 0;

        while (currentParentId != null && currentDepth < maxDepth) {
            if (currentParentId.equals(ancestorId)) {
                return true;
            }

            Optional<Category> parent = categoryRepository.findById(currentParentId);
            if (parent.isEmpty()) {
                break;
            }

            currentParentId = parent.get().getParentId();
            currentDepth++;
        }

        return false;
    }

    private void updateDescendantLevels(UUID categoryId) {
        List<Category> subcategories = findSubcategories(categoryId);

        for (Category subcategory : subcategories) {
            subcategory.setLevel(calculateCategoryLevel(subcategory.getParentId()));
            categoryRepository.save(subcategory);

            // Recursively update descendants
            updateDescendantLevels(subcategory.getId());
        }
    }

    // ====== VALIDATION METHODS ======

    private List<String> validateForCreation(Category category) {
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
            if (categoryRepository.findById(category.getParentId()).isEmpty()) {
                errors.add("Parent category does not exist");
            }

            // Check maximum depth (e.g., limit to 5 levels)
            int parentLevel = calculateCategoryLevel(category.getParentId());
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

    private List<String> validateForUpdate(Category category, UUID newParentId) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        if (category.getName() != null && category.getName().trim().isEmpty()) {
            errors.add("Category name cannot be empty");
        }

        if (category.getName() != null && category.getName().length() > 100) {
            errors.add("Category name cannot exceed 100 characters");
        }

        // Parent validation for updates
        if (newParentId != null && !canMoveCategory(category.getId(), newParentId)) {
            errors.add("Cannot move category to the specified parent (would create circular reference)");
        }

        if (newParentId != null && categoryRepository.findById(newParentId).isEmpty()) {
            errors.add("New parent category does not exist");
        }

        return errors;
    }

    private List<String> validateForDeletion(Category category) {
        List<String> errors = new ArrayList<>();

        // Check if category has subcategories
        List<Category> subcategories = findSubcategories(category.getId());
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