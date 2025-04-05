package com.Ecommerce.Product_Service.Services;


import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Repositories.CategoryRepository;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findCategoryById(UUID id) {
        return categoryRepository.findById(id);
    }

    public List<Category> findRootCategories() {
        return categoryRepository.findByParentId(null);
    }

    public List<Category> findSubcategories(UUID parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public List<Category> findCategoriesByLevel(Integer level) {
        return categoryRepository.findByLevel(level);
    }

    @Transactional
    public Category addCategory(Category category) {
        // Validate category
        validateCategory(category);

        // Set creation timestamp
        category.setCreatedAt(LocalDateTime.now());

        // Calculate level
        if (category.getParentId() == null) {
            category.setLevel(0);
        } else {
            Optional<Category> parent = categoryRepository.findById(category.getParentId());
            if (parent.isEmpty()) {
                throw new IllegalArgumentException("Parent category not found");
            }
            category.setLevel(parent.get().getLevel() + 1);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public Optional<Category> updateCategory(UUID id, Category updatedCategory) {
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    if (updatedCategory.getName() != null) {
                        existingCategory.setName(updatedCategory.getName());
                    }
                    if (updatedCategory.getDescription() != null) {
                        existingCategory.setDescription(updatedCategory.getDescription());
                    }
                    if (updatedCategory.getImageUrl() != null) {
                        existingCategory.setImageUrl(updatedCategory.getImageUrl());
                    }

                    // Handle parent change (which impacts level)
                    if (updatedCategory.getParentId() != null &&
                            !updatedCategory.getParentId().equals(existingCategory.getParentId())) {

                        // Check for circular reference
                        if (updatedCategory.getParentId().equals(id)) {
                            throw new IllegalArgumentException("Category cannot be its own parent");
                        }

                        // Check if new parent exists
                        Optional<Category> newParent = categoryRepository.findById(updatedCategory.getParentId());
                        if (newParent.isEmpty()) {
                            throw new IllegalArgumentException("Parent category not found");
                        }

                        // Check if new parent would create a cycle
                        if (isDescendant(id, updatedCategory.getParentId())) {
                            throw new IllegalArgumentException("Cannot create circular hierarchy");
                        }

                        existingCategory.setParentId(updatedCategory.getParentId());
                        existingCategory.setLevel(newParent.get().getLevel() + 1);

                        // Update all child levels
                        updateDescendantLevels(existingCategory);
                    }

                    return categoryRepository.save(existingCategory);
                });
    }

    @Transactional
    public void deleteCategory(UUID id) {
        // Check if category has subcategories
        List<Category> subcategories = categoryRepository.findByParentId(id);
        if (!subcategories.isEmpty()) {
            throw new IllegalStateException("Cannot delete category with subcategories");
        }

        // Remove category from products
        categoryRepository.findById(id).ifPresent(category -> {
            for (Product product : category.getProducts()) {
                product.getCategories().remove(category);
                productRepository.save(product);
            }
        });

        categoryRepository.deleteById(id);
    }

    public String getFullPath(UUID categoryId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return "";
        }

        List<String> pathParts = new ArrayList<>();
        Category current = categoryOpt.get();
        pathParts.add(current.getName());

        while (current.getParentId() != null) {
            Optional<Category> parentOpt = categoryRepository.findById(current.getParentId());
            if (parentOpt.isEmpty()) {
                break;
            }
            current = parentOpt.get();
            pathParts.add(0, current.getName());
        }

        return String.join(" > ", pathParts);
    }

    @Transactional
    public Optional<Category> addProductToCategory(UUID categoryId, UUID productId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (categoryOpt.isPresent() && productOpt.isPresent()) {
            Category category = categoryOpt.get();
            Product product = productOpt.get();

            category.getProducts().add(product);
            product.getCategories().add(category);

            productRepository.save(product);
            return Optional.of(categoryRepository.save(category));
        }

        return Optional.empty();
    }

    @Transactional
    public Optional<Category> removeProductFromCategory(UUID categoryId, UUID productId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (categoryOpt.isPresent() && productOpt.isPresent()) {
            Category category = categoryOpt.get();
            Product product = productOpt.get();

            category.getProducts().removeIf(p -> p.getId().equals(productId));
            product.getCategories().removeIf(c -> c.getId().equals(categoryId));

            productRepository.save(product);
            return Optional.of(categoryRepository.save(category));
        }

        return Optional.empty();
    }

    public Map<String, Object> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();

        // First, group by level
        Map<Integer, List<Category>> categoryByLevel = allCategories.stream()
                .collect(Collectors.groupingBy(Category::getLevel));

        // Build the tree starting with root categories
        List<Map<String, Object>> rootNodes = new ArrayList<>();
        List<Category> rootCategories = categoryByLevel.getOrDefault(0, Collections.emptyList());

        for (Category root : rootCategories) {
            rootNodes.add(buildCategoryNode(root, allCategories));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("categories", rootNodes);
        return result;
    }

    private Map<String, Object> buildCategoryNode(Category category, List<Category> allCategories) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", category.getId());
        node.put("name", category.getName());
        node.put("level", category.getLevel());

        List<Map<String, Object>> children = allCategories.stream()
                .filter(c -> category.getId().equals(c.getParentId()))
                .map(child -> buildCategoryNode(child, allCategories))
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            node.put("children", children);
        }

        return node;
    }

    private boolean isDescendant(UUID categoryId, UUID potentialAncestorId) {
        Queue<UUID> queue = new LinkedList<>();
        queue.add(categoryId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            List<Category> children = categoryRepository.findByParentId(current);

            for (Category child : children) {
                if (child.getId().equals(potentialAncestorId)) {
                    return true;
                }
                queue.add(child.getId());
            }
        }

        return false;
    }

    private void updateDescendantLevels(Category category) {
        int parentLevel = category.getLevel();
        List<Category> directChildren = categoryRepository.findByParentId(category.getId());

        for (Category child : directChildren) {
            child.setLevel(parentLevel + 1);
            categoryRepository.save(child);
            updateDescendantLevels(child);
        }
    }

    private void validateCategory(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        // Check for circular reference
        if (category.getId() != null && category.getId().equals(category.getParentId())) {
            throw new IllegalArgumentException("Category cannot be its own parent");
        }
    }
}