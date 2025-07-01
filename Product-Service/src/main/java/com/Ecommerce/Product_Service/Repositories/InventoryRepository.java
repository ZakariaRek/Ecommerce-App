package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    // Existing methods
    List<Inventory> findByQuantityLessThanAndProduct_Status(Integer quantity, ProductStatus status);
    List<Inventory> findByLastRestockedBefore(LocalDateTime date);

    // New methods for enhanced CRUD operations

    /**
     * Find inventory items by warehouse location
     */
    List<Inventory> findByWarehouseLocationIgnoreCase(String warehouseLocation);

    /**
     * Find inventory items with quantity less than their low stock threshold
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.lowStockThreshold")
    List<Inventory> findLowStockItems();

    /**
     * Find inventory items with zero quantity (out of stock)
     */
    List<Inventory> findByQuantity(Integer quantity);

    /**
     * Find inventory items by quantity range
     */
    List<Inventory> findByQuantityBetween(Integer minQuantity, Integer maxQuantity);

    /**
     * Find inventory items that need restocking (based on threshold)
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity < i.lowStockThreshold AND i.product.status = :status")
    List<Inventory> findItemsNeedingRestock(@Param("status") ProductStatus status);

    /**
     * Find inventory by product SKU
     */
    @Query("SELECT i FROM Inventory i WHERE i.product.sku = :sku")
    Optional<Inventory> findByProductSku(@Param("sku") String sku);

    /**
     * Find inventory by product name (case insensitive)
     */
    @Query("SELECT i FROM Inventory i WHERE LOWER(i.product.name) LIKE LOWER(CONCAT('%', :productName, '%'))")
    List<Inventory> findByProductNameContainingIgnoreCase(@Param("productName") String productName);

    /**
     * Find all inventory items with products of a specific status
     */
    @Query("SELECT i FROM Inventory i WHERE i.product.status = :status")
    List<Inventory> findByProductStatus(@Param("status") ProductStatus status);

    /**
     * Find inventory items restocked after a specific date
     */
    List<Inventory> findByLastRestockedAfter(LocalDateTime date);

    /**
     * Find inventory items by warehouse location and low stock status
     */
    @Query("SELECT i FROM Inventory i WHERE i.warehouseLocation = :location AND i.quantity <= i.lowStockThreshold")
    List<Inventory> findLowStockItemsByWarehouse(@Param("location") String location);

    /**
     * Count total inventory items
     */
    @Query("SELECT COUNT(i) FROM Inventory i")
    Long countTotalInventoryItems();

    /**
     * Count low stock items
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity <= i.lowStockThreshold")
    Long countLowStockItems();

    /**
     * Count out of stock items
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity = 0")
    Long countOutOfStockItems();

    /**
     * Get total quantity across all inventory
     */
    @Query("SELECT SUM(i.quantity) FROM Inventory i")
    Long getTotalQuantityInStock();

    /**
     * Get total quantity by warehouse
     */
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.warehouseLocation = :location")
    Long getTotalQuantityByWarehouse(@Param("location") String location);

    /**
     * Find inventory items with specific low stock threshold
     */
    List<Inventory> findByLowStockThreshold(Integer threshold);

    /**
     * Find inventory items with threshold greater than or equal to specified value
     */
    List<Inventory> findByLowStockThresholdGreaterThanEqual(Integer threshold);

    /**
     * Check if inventory exists for a specific product
     */
    boolean existsByProductId(UUID productId);

    /**
     * Find inventory items by multiple warehouse locations
     */
    List<Inventory> findByWarehouseLocationIn(List<String> locations);

    /**
     * Find inventory with quantity greater than specified amount
     */
    List<Inventory> findByQuantityGreaterThan(Integer quantity);

    /**
     * Find recently restocked items (within specified days)
     */
    @Query("SELECT i FROM Inventory i WHERE i.lastRestocked >= :cutoffDate ORDER BY i.lastRestocked DESC")
    List<Inventory> findRecentlyRestockedItems(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find inventory items that haven't been restocked for a long time
     */
    @Query("SELECT i FROM Inventory i WHERE i.lastRestocked <= :cutoffDate ORDER BY i.lastRestocked ASC")
    List<Inventory> findStaleInventoryItems(@Param("cutoffDate") LocalDateTime cutoffDate);
}