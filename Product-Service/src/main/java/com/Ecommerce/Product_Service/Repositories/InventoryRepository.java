package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findByQuantityLessThanAndProduct_Status(Integer quantity, ProductStatus status);
    List<Inventory> findByLastRestockedBefore(LocalDateTime date);
}