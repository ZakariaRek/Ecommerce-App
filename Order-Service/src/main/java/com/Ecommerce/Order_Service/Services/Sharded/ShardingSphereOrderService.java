package com.Ecommerce.Order_Service.Services.Sharded;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.KafkaProducers.OrderKafkaService;
import com.Ecommerce.Order_Service.Repositories.OrderItemRepository;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import com.Ecommerce.Order_Service.Services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced Order Service that leverages ShardingSphere capabilities
 */
@Service
@Primary
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ShardingSphereOrderService extends OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderKafkaService kafkaService;

    @Override
    public Order createOrder(String userId, UUID cartId, UUID billingAddressId, UUID shippingAddressId) {
        log.info("🔀 SHARDINGSPHERE: Creating order for user {} (will be auto-sharded)", userId);

        // ShardingSphere will automatically route this to the correct shard based on user_id
        Order order = super.createOrder(userId, cartId, billingAddressId, shippingAddressId);

        log.info("🔀 SHARDINGSPHERE: Order {} created successfully for user {}", order.getId(), userId);
        return order;
    }

    @Override
    public Order getOrderById(UUID orderId) {
        log.debug("🔀 SHARDINGSPHERE: Getting order {} (may search across shards)", orderId);

        // ShardingSphere will handle the routing automatically
        // If the order is not found on the calculated shard, it may search other shards
        return super.getOrderById(orderId);
    }

    @Override
    public List<Order> getOrdersByUserId(UUID userId) {
        log.debug("🔀 SHARDINGSPHERE: Getting orders for user {} (will use single shard)", userId);

        // This will be routed to a single shard based on user_id
        return super.getOrdersByUserId(userId);
    }

    /**
     * Force query to execute on a specific shard using ShardingSphere Hint
     */
    public List<Order> getOrdersWithHint(UUID userId, String shardName) {
        log.info("🔀 SHARDINGSPHERE: Getting orders for user {} with hint to use shard {}", userId, shardName);

        try (HintManager hintManager = HintManager.getInstance()) {
            // Force execution on specific shard
            hintManager.addDatabaseShardingValue("orders", shardName);

            return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    /**
     * Execute read-only query with read/write splitting hint
     */
    public List<Order> getOrdersReadOnly(UUID userId) {
        log.debug("🔀 SHARDINGSPHERE: Getting orders for user {} (read-only)", userId);

        try (HintManager hintManager = HintManager.getInstance()) {
            // Hint that this is a read-only operation
            hintManager.setWriteRouteOnly();

            return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    /**
     * Batch operation that benefits from ShardingSphere's optimization
     */
    @Transactional
    public List<Order> createOrdersBatch(List<OrderCreationRequest> requests) {
        log.info("🔀 SHARDINGSPHERE: Creating {} orders in batch", requests.size());

        // ShardingSphere will optimize batch operations across shards
        List<Order> orders = new ArrayList<>();

        for (OrderCreationRequest request : requests) {
            Order order = createOrder(request.getUserId(), request.getCartId(),
                    request.getBillingAddressId(), request.getShippingAddressId());
            orders.add(order);
        }

        log.info("🔀 SHARDINGSPHERE: Batch created {} orders successfully", orders.size());
        return orders;
    }

    /**
     * Cross-shard aggregation query
     */
    public OrderStatistics getOrderStatisticsAcrossShards() {
        log.info("🔀 SHARDINGSPHERE: Calculating order statistics across all shards");

        // ShardingSphere will execute this across all shards and aggregate results
        long totalOrders = orderRepository.count();

        // Custom aggregation queries
        List<Order> allOrders = orderRepository.findAll();

        BigDecimal totalRevenue = allOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .count();

        return OrderStatistics.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .pendingOrders(pendingOrders)
                .build();
    }



    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OrderCreationRequest {
        private String userId;
        private UUID cartId;
        private UUID billingAddressId;
        private UUID shippingAddressId;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OrderStatistics {
        private long totalOrders;
        private BigDecimal totalRevenue;
        private long pendingOrders;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OrderSummary {
        private UUID orderId;
        private UUID userId;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private int itemCount;
    }
}
