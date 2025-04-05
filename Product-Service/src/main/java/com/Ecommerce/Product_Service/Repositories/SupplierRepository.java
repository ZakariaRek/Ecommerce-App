package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByRatingGreaterThanEqual(BigDecimal minRating);
    List<Supplier> findByNameContainingIgnoreCase(String name);
}