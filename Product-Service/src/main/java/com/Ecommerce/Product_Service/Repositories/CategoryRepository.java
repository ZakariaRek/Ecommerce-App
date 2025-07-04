package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // ====== BASIC FINDER METHODS ======

    /**
     * Find categories by parent ID (null for root categories)
     */
    List<Category> findByParentId(UUID parentId);

    /**
     * Find root categories (where parent is null)
     */
    List<Category> findByParentIdIsNull();

    /**
     * Find categories by level
     */
    List<Category> findByLevel(Integer level);

    /**
     * Find categories by name (case insensitive)
     */
    List<Category> findByNameContainingIgnoreCase(String name);

    /**
     * Check if category name exists (for validation)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if category name exists excluding current category (for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") UUID id);

    // ====== HIERARCHY QUERIES ======

    /**
     * Find all subcategories of a parent (recursive)
     */
    @Query("SELECT c FROM Category c WHERE c.parentId = :parentId")
    List<Category> findSubcategories(@Param("parentId") UUID parentId);

    /**
     * Find all descendant categories (all levels down)
     */
    @Query(value = """
        WITH RECURSIVE category_hierarchy AS (
            SELECT id, name, description, parent_id, level, image_url, created_at, updated_at
            FROM categories 
            WHERE parent_id = :parentId
            UNION ALL
            SELECT c.id, c.name, c.description, c.parent_id, c.level, c.image_url, c.created_at, c.updated_at
            FROM categories c
            INNER JOIN category_hierarchy ch ON c.parent_id = ch.id
        )
        SELECT * FROM category_hierarchy
        """, nativeQuery = true)
    List<Category> findAllDescendants(@Param("parentId") UUID parentId);

    /**
     * Get category path from root to specific category
     */
    @Query(value = """
        WITH RECURSIVE category_path AS (
            SELECT id, name, parent_id, level, 0 as depth
            FROM categories 
            WHERE id = :categoryId
            UNION ALL
            SELECT c.id, c.name, c.parent_id, c.level, cp.depth + 1
            FROM categories c
            INNER JOIN category_path cp ON c.id = cp.parent_id
        )
        SELECT c.* FROM categories c 
        WHERE c.id IN (SELECT id FROM category_path)
        ORDER BY (SELECT depth FROM category_path WHERE category_path.id = c.id) DESC
        """, nativeQuery = true)
    List<Category> findCategoryPath(@Param("categoryId") UUID categoryId);

    // ====== PRODUCT-RELATED QUERIES ======

    /**
     * Find categories that have products
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.products p")
    List<Category> findCategoriesWithProducts();

    /**
     * Find categories without products
     */
    @Query("SELECT c FROM Category c WHERE c.products IS EMPTY OR c.products IS NULL")
    List<Category> findEmptyCategories();

    /**
     * Count products in category
     */
    @Query("SELECT COUNT(p) FROM Category c JOIN c.products p WHERE c.id = :categoryId")
    Long countProductsInCategory(@Param("categoryId") UUID categoryId);

    /**
     * Find categories by product ID
     */
    @Query("SELECT c FROM Category c JOIN c.products p WHERE p.id = :productId")
    List<Category> findCategoriesByProductId(@Param("productId") UUID productId);

    // ====== STATISTICS AND ANALYTICS ======

    /**
     * Get category statistics
     */
    @Query("""
        SELECT new map(
            COUNT(c) as totalCategories,
            COUNT(CASE WHEN c.parentId IS NULL THEN 1 END) as rootCategories,
            MAX(c.level) as maxDepth,
            COALESCE(SUM(SIZE(c.products)), 0) as totalProducts,
            CASE WHEN COUNT(c) > 0 THEN COALESCE(SUM(SIZE(c.products)), 0) / COUNT(c) ELSE 0 END as avgProductsPerCategory
        )
        FROM Category c
        """)
    Object getCategoryStatistics();

    /**
     * Find most popular categories (by product count)
     */
    @Query("""
        SELECT c FROM Category c 
        LEFT JOIN c.products p 
        GROUP BY c 
        ORDER BY COUNT(p) DESC
        """)
    List<Category> findMostPopularCategories();

    /**
     * Find categories by level range
     */
    @Query("SELECT c FROM Category c WHERE c.level BETWEEN :minLevel AND :maxLevel ORDER BY c.level, c.name")
    List<Category> findCategoriesByLevelRange(@Param("minLevel") Integer minLevel, @Param("maxLevel") Integer maxLevel);

    // ====== VALIDATION QUERIES ======

    /**
     * Check if moving a category would create circular reference
     */
    @Query(value = """
        WITH RECURSIVE category_ancestors AS (
            SELECT id, parent_id, 0 as depth
            FROM categories 
            WHERE id = :newParentId
            UNION ALL
            SELECT c.id, c.parent_id, ca.depth + 1
            FROM categories c
            INNER JOIN category_ancestors ca ON c.id = ca.parent_id
            WHERE ca.depth < 10
        )
        SELECT COUNT(*) > 0 FROM category_ancestors WHERE id = :categoryId
        """, nativeQuery = true)
    boolean wouldCreateCircularReference(@Param("categoryId") UUID categoryId, @Param("newParentId") UUID newParentId);

    /**
     * Check if category has subcategories
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.parentId = :categoryId")
    boolean hasSubcategories(@Param("categoryId") UUID categoryId);

    // ====== SEARCH AND FILTERING ======

    /**
     * Search categories by name and description
     */
    @Query("""
        SELECT c FROM Category c 
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
        OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY c.level, c.name
        """)
    List<Category> searchCategories(@Param("searchTerm") String searchTerm);

    /**
     * Find categories with specific criteria
     */
    @Query("""
        SELECT c FROM Category c 
        WHERE (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:level IS NULL OR c.level = :level)
        AND (:hasProducts IS NULL OR 
             (:hasProducts = true AND SIZE(c.products) > 0) OR 
             (:hasProducts = false AND SIZE(c.products) = 0))
        ORDER BY c.level, c.name
        """)
    List<Category> findCategoriesWithCriteria(
            @Param("name") String name,
            @Param("level") Integer level,
            @Param("hasProducts") Boolean hasProducts
    );

    // ====== BATCH OPERATIONS ======

    /**
     * Find categories by multiple IDs with products loaded
     */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.products WHERE c.id IN :ids")
    List<Category> findCategoriesWithProducts(@Param("ids") List<UUID> ids);

    /**
     * Update parent for multiple categories
     */
    @Query("UPDATE Category c SET c.parentId = :newParentId WHERE c.id IN :categoryIds")
    void updateParentForCategories(@Param("categoryIds") List<UUID> categoryIds, @Param("newParentId") UUID newParentId);

    /**
     * Delete categories by IDs (use with caution)
     */
    void deleteByIdIn(List<UUID> ids);
}

// ====== UPDATED CATEGORY SERVICE ======
// Update your CategoryService to use the correct JpaRepository method:

// In CategoryService.java, replace the findByIds method:
/*
public List<Category> findByIds(List<UUID> ids) {
    return categoryRepository.findAllById(ids); // Use the built-in JpaRepository method
}
*/