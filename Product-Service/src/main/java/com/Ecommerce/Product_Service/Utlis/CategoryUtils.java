package com.Ecommerce.Product_Service.Utlis;



import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Services.CategoryService;

import java.util.*;
import java.util.stream.Collectors;

public class CategoryUtils {

    /**
     * Generate a URL-friendly slug from category name
     */
    public static String generateSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }

        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Replace multiple hyphens with single
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }

    /**
     * Check if a category can be moved to a new parent (prevents circular references)
     * Using provided category map for efficiency
     */
    public static boolean canMoveCategory(UUID categoryId, UUID newParentId, Map<UUID, Category> categoryMap) {
        if (categoryId == null || newParentId == null) {
            return true; // Can always move to root (null parent)
        }

        if (categoryId.equals(newParentId)) {
            return false; // Cannot be its own parent
        }

        // Check if newParent is a descendant of category (would create circular reference)
        return !isDescendant(newParentId, categoryId, categoryMap);
    }

    /**
     * Check if a category is a descendant of another category
     */
    public static boolean isDescendant(UUID potentialDescendantId, UUID ancestorId, Map<UUID, Category> categoryMap) {
        if (potentialDescendantId == null || ancestorId == null || categoryMap == null) {
            return false;
        }

        Category potentialDescendant = categoryMap.get(potentialDescendantId);
        if (potentialDescendant == null) {
            return false;
        }

        UUID currentParentId = potentialDescendant.getParentId();
        int maxDepth = 10; // Prevent infinite loops
        int currentDepth = 0;

        while (currentParentId != null && currentDepth < maxDepth) {
            if (currentParentId.equals(ancestorId)) {
                return true;
            }

            Category parent = categoryMap.get(currentParentId);
            if (parent == null) {
                break;
            }

            currentParentId = parent.getParentId();
            currentDepth++;
        }

        return false;
    }

    /**
     * Calculate the level of a category based on its parent hierarchy
     */
    public static int calculateLevel(UUID categoryId, CategoryService categoryMap) {
        if (categoryId == null || categoryMap == null) {
            return 0;
        }

        Optional<Category> category = categoryMap.findCategoryById(categoryId);
        if (category.isEmpty() || category.get().getParentId() == null) {
            return 0;
        }

        int level = 0;
        UUID currentParentId = category.get().getParentId();
        int maxDepth = 10; // Prevent infinite loops

        while (currentParentId != null && level < maxDepth) {
            level++;
            Optional<Category> parent = categoryMap.findCategoryById(currentParentId);
            if (parent.isEmpty()) {
                break;
            }
            currentParentId = parent.get().getParentId();
        }

        return level;
    }

    /**
     * Get all descendant category IDs (recursive)
     */
    public static List<UUID> getAllDescendantIds(UUID categoryId, Map<UUID, List<Category>> subcategoryMap) {
        List<UUID> descendants = new ArrayList<>();

        if (categoryId == null || subcategoryMap == null) {
            return descendants;
        }

        List<Category> subcategories = subcategoryMap.get(categoryId);
        if (subcategories != null) {
            for (Category subcategory : subcategories) {
                descendants.add(subcategory.getId());
                descendants.addAll(getAllDescendantIds(subcategory.getId(), subcategoryMap));
            }
        }

        return descendants;
    }

    /**
     * Get all ancestor category IDs (from root to immediate parent)
     */
    public static List<UUID> getAllAncestorIds(UUID categoryId, Map<UUID, Category> categoryMap) {
        List<UUID> ancestors = new ArrayList<>();

        if (categoryId == null || categoryMap == null) {
            return ancestors;
        }

        Category category = categoryMap.get(categoryId);
        if (category == null) {
            return ancestors;
        }

        UUID currentParentId = category.getParentId();
        int maxDepth = 10; // Prevent infinite loops
        int currentDepth = 0;

        while (currentParentId != null && currentDepth < maxDepth) {
            ancestors.add(0, currentParentId); // Add to beginning to maintain order from root

            Category parent = categoryMap.get(currentParentId);
            if (parent == null) {
                break;
            }

            currentParentId = parent.getParentId();
            currentDepth++;
        }

        return ancestors;
    }

    /**
     * Sort categories by level and then by name
     */
    public static List<Category> sortCategories(List<Category> categories) {
        if (categories == null) {
            return new ArrayList<>();
        }

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

    /**
     * Build a map of category ID to full path
     */
    public static Map<UUID, String> buildPathMap(List<Category> categories) {
        if (categories == null) {
            return new HashMap<>();
        }

        Map<UUID, String> pathMap = new HashMap<>();
        Map<UUID, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        for (Category category : categories) {
            String path = buildFullPath(category, categoryMap);
            pathMap.put(category.getId(), path);
        }

        return pathMap;
    }

    /**
     * Build full path string for a category using category map for efficiency
     */
    public static String buildFullPath(Category category, Map<UUID, Category> categoryMap) {
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

    /**
     * Build full path for a single category (without pre-built map)
     */
    public static String buildFullPath(Category category, List<Category> allCategories) {
        if (category == null || allCategories == null) return "";

        Map<UUID, Category> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, cat -> cat));

        return buildFullPath(category, categoryMap);
    }

    /**
     * Validate category hierarchy to prevent cycles
     */
    public static boolean isValidHierarchy(List<Category> categories) {
        if (categories == null) return true;

        Map<UUID, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category));

        for (Category category : categories) {
            if (hasCircularReference(category, categoryMap, new HashSet<>())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check for circular references in category hierarchy
     */
    private static boolean hasCircularReference(Category category, Map<UUID, Category> categoryMap, Set<UUID> visited) {
        if (category == null || category.getId() == null) {
            return false;
        }

        if (visited.contains(category.getId())) {
            return true; // Circular reference detected
        }

        visited.add(category.getId());

        if (category.getParentId() != null && categoryMap.containsKey(category.getParentId())) {
            Category parent = categoryMap.get(category.getParentId());
            if (hasCircularReference(parent, categoryMap, visited)) {
                return true;
            }
        }

        visited.remove(category.getId());
        return false;
    }

    /**
     * Group categories by their parent ID
     */
    public static Map<UUID, List<Category>> groupByParent(List<Category> categories) {
        if (categories == null) {
            return new HashMap<>();
        }

        return categories.stream()
                .filter(cat -> cat.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));
    }

    /**
     * Get root categories (those without parent)
     */
    public static List<Category> getRootCategories(List<Category> categories) {
        if (categories == null) {
            return new ArrayList<>();
        }

        return categories.stream()
                .filter(cat -> cat.getParentId() == null)
                .collect(Collectors.toList());
    }

    /**
     * Get categories at a specific level
     */
    public static List<Category> getCategoriesByLevel(List<Category> categories, int level) {
        if (categories == null) {
            return new ArrayList<>();
        }

        return categories.stream()
                .filter(cat -> cat.getLevel() != null && cat.getLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * Find maximum depth in category hierarchy
     */
    public static int getMaxDepth(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return 0;
        }

        return categories.stream()
                .mapToInt(cat -> cat.getLevel() != null ? cat.getLevel() : 0)
                .max()
                .orElse(0);
    }

    /**
     * Count categories at each level
     */
    public static Map<Integer, Long> countByLevel(List<Category> categories) {
        if (categories == null) {
            return new HashMap<>();
        }

        return categories.stream()
                .collect(Collectors.groupingBy(
                        cat -> cat.getLevel() != null ? cat.getLevel() : 0,
                        Collectors.counting()
                ));
    }

    /**
     * Flatten category tree to a list with indentation for display
     */
    public static List<String> flattenToDisplayList(List<Category> rootCategories, Map<UUID, List<Category>> subcategoryMap) {
        List<String> flatList = new ArrayList<>();

        if (rootCategories == null) {
            return flatList;
        }

        for (Category root : rootCategories) {
            addCategoryToFlatList(root, flatList, 0, subcategoryMap);
        }

        return flatList;
    }

    /**
     * Recursively add categories to flat list with level indication
     */
    private static void addCategoryToFlatList(Category category, List<String> flatList, int indentLevel, Map<UUID, List<Category>> subcategoryMap) {
        // Add indentation to name for display purposes
        String indent = "  ".repeat(indentLevel);
        String prefix = indentLevel > 0 ? "└ " : "";
        String displayName = indent + prefix + category.getName();

        flatList.add(displayName);

        // Recursively add subcategories
        List<Category> subcategories = subcategoryMap.get(category.getId());
        if (subcategories != null) {
            subcategories.stream()
                    .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
                    .forEach(subcategory -> addCategoryToFlatList(subcategory, flatList, indentLevel + 1, subcategoryMap));
        }
    }

    /**
     * Create breadcrumb path from category to root
     */
    public static List<String> createBreadcrumbs(UUID categoryId, Map<UUID, Category> categoryMap) {
        List<String> breadcrumbs = new ArrayList<>();

        if (categoryId == null || categoryMap == null) {
            return breadcrumbs;
        }

        Category current = categoryMap.get(categoryId);
        int maxDepth = 10; // Prevent infinite loops
        int currentDepth = 0;

        while (current != null && currentDepth < maxDepth) {
            breadcrumbs.add(0, current.getName()); // Add to beginning

            if (current.getParentId() != null) {
                current = categoryMap.get(current.getParentId());
            } else {
                current = null;
            }
            currentDepth++;
        }

        return breadcrumbs;
    }

    /**
     * Validate category name
     */
    public static List<String> validateCategoryName(String name) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Category name is required");
        } else {
            if (name.length() > 100) {
                errors.add("Category name cannot exceed 100 characters");
            }
            if (name.length() < 2) {
                errors.add("Category name must be at least 2 characters");
            }
        }

        return errors;
    }

    /**
     * Check if category has products
     */
    public static boolean hasProducts(Category category) {
        return category != null &&
                category.getProducts() != null &&
                !category.getProducts().isEmpty();
    }

    /**
     * Count total products in category and all subcategories
     */
    public static int getTotalProductCount(UUID categoryId, Map<UUID, Category> categoryMap, Map<UUID, List<Category>> subcategoryMap) {
        if (categoryId == null || categoryMap == null) {
            return 0;
        }

        Category category = categoryMap.get(categoryId);
        if (category == null) {
            return 0;
        }

        int count = category.getProducts() != null ? category.getProducts().size() : 0;

        // Add products from subcategories
        List<UUID> descendantIds = getAllDescendantIds(categoryId, subcategoryMap);
        for (UUID descendantId : descendantIds) {
            Category descendant = categoryMap.get(descendantId);
            if (descendant != null && descendant.getProducts() != null) {
                count += descendant.getProducts().size();
            }
        }

        return count;
    }
}