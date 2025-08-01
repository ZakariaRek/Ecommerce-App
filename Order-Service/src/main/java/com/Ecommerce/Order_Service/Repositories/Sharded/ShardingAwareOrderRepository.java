package com.Ecommerce.Order_Service.Repositories.Sharded;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced Order Repository with ShardingSphere-optimized queries
 */
@Repository
public interface ShardingAwareOrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find orders by user ID - will be routed to single shard
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find orders by user ID and status - single shard operation
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, OrderStatus status);

    /**
     * Find recent orders for a user - optimized for single shard
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") UUID userId);


    /**
     * Cross-shard aggregation query
     * ShardingSphere will execute this across all shards and merge results
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status")
    BigDecimal getTotalAmountByStatus(@Param("status") OrderStatus status);

    /**
     * Date range query that may span multiple shards
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * User-specific date range query - optimized for single shard
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersByUserAndDateRange(@Param("userId") UUID userId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Count orders by status across all shards
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countOrdersByStatus(@Param("status") OrderStatus status);

    /**
     * Count orders for a specific user - single shard
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    Long countOrdersByUserId(@Param("userId") UUID userId);

    /**
     * Find orders with total amount greater than threshold - cross-shard
     */
    @Query("SELECT o FROM Order o WHERE o.totalAmount > :threshold ORDER BY o.totalAmount DESC")
    List<Order> findHighValueOrders(@Param("threshold") BigDecimal threshold);

    /**
     * Complex aggregation query with grouping
     */
    @Query("""
        SELECT o.status, COUNT(o), SUM(o.totalAmount) 
        FROM Order o 
        GROUP BY o.status
        """)
    List<Object[]> getOrderStatisticsByStatus();

    /**
     * User-specific statistics - single shard operation
     */
    @Query("""
        SELECT o.status, COUNT(o), SUM(o.totalAmount) 
        FROM Order o 
        WHERE o.userId = :userId 
        GROUP BY o.status
        """)
    List<Object[]> getOrderStatisticsByUserAndStatus(@Param("userId") UUID userId);
}