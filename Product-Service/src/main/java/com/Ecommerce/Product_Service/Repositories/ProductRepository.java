package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);
    List<Product> findByNameContainingIgnoreCase(String name);
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.suppliers WHERE p.id IN :productIds")
    List<Product> findAllByIdWithSuppliers(@Param("productIds") List<UUID> productIds);

    @Query("SELECT p FROM Product p JOIN p.suppliers s WHERE s.id = :supplierId")
    List<Product> findBySupplier(@Param("supplierId") UUID supplierId);

    @Modifying
    @Query("DELETE FROM Product p WHERE p.id IN (SELECT ps.id FROM Product ps JOIN ps.suppliers s WHERE s.id = :supplierId AND ps.id IN :productIds)")
    void removeSupplierFromProducts(@Param("supplierId") UUID supplierId, @Param("productIds") List<UUID> productIds);

}