package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByParentId(UUID parentId);
    List<Category> findByLevel(Integer level);
}
