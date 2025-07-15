package com.Ecommerce.Order_Service.Repositories;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find all orders for a specific user ordered by creation date
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all orders for a specific user with a specific status
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, OrderStatus status);

    /**
     * Find order by ID with items eagerly loaded
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);

    /**
     * Find orders by cart ID
     */
    Optional<Order> findByCartId(UUID cartId);

    /**
     * Find orders created between dates
     */
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find pending orders older than specified date
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :cutoffDate")
    List<Order> findPendingOrdersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count orders by status for a user
     */
    Long countByUserIdAndStatus(UUID userId, OrderStatus status);

    /**
     * Get total amount for completed orders by user
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId AND o.status IN ('DELIVERED', 'COMPLETED')")
    BigDecimal getTotalSpentByUser(@Param("userId") UUID userId);

    /**
     * Find recent orders for a user (with limit)
     */
    @Query(value = "SELECT * FROM orders WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<Order> findRecentOrdersByUserId(@Param("userId") UUID userId);

    /**
     * Check if user has any orders
     */
    Boolean existsByUserId(UUID userId);

    /**
     * Find orders by multiple statuses
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusIn(@Param("userId") UUID userId, @Param("statuses") List<OrderStatus> statuses);
}