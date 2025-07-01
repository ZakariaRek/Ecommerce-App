package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Find root categories (those without a parent)
    List<Category> findByParentIdIsNull();

    // Find subcategories of a specific parent
    List<Category> findByParentId(UUID parentId);

    // Find categories by level
    List<Category> findByLevel(Integer level);

    // Find categories by name (case-insensitive)
    List<Category> findByNameContainingIgnoreCase(String name);

    // Find categories by parent and order by name
    List<Category> findByParentIdOrderByNameAsc(UUID parentId);

    // Find all categories ordered by level and name
    List<Category> findAllByOrderByLevelAscNameAsc();

    // Count subcategories
    long countByParentId(UUID parentId);
}
