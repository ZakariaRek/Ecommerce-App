package com.Ecommerce.Order_Service.Repositories;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private UUID testUserId1;
    private UUID testUserId2;
    private Order order1;
    private Order order2;
    private Order order3;

    @BeforeEach
    void setUp() {
        testUserId1 = UUID.randomUUID();
        testUserId2 = UUID.randomUUID();

        // Create test orders
        order1 = createTestOrder(testUserId1, OrderStatus.PENDING, LocalDateTime.now().minusDays(3));
        order2 = createTestOrder(testUserId1, OrderStatus.SHIPPED, LocalDateTime.now().minusDays(1));
        order3 = createTestOrder(testUserId2, OrderStatus.DELIVERED, LocalDateTime.now().minusHours(2));

        // Persist orders
        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        entityManager.persistAndFlush(order3);
        entityManager.clear();
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_WithValidUserId_ReturnsOrdersInDescendingOrder() {
        // When
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId1);

        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getId()).isEqualTo(order2.getId()); // Most recent first
        assertThat(orders.get(1).getId()).isEqualTo(order1.getId());
        assertThat(orders.get(0).getCreatedAt()).isAfter(orders.get(1).getCreatedAt());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_WithNonExistentUserId_ReturnsEmptyList() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();

        // When
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(nonExistentUserId);

        // Then
        assertThat(orders).isEmpty();
    }

    @Test
    void findRecentOrdersByUserId_WithValidUserId_ReturnsOrdersInDescendingOrder() {
        // When
        List<Order> orders = orderRepository.findRecentOrdersByUserId(testUserId1);

        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getId()).isEqualTo(order2.getId()); // Most recent first
        assertThat(orders.get(1).getId()).isEqualTo(order1.getId());
    }

    @Test
    void findByUserIdAndStatusOrderByCreatedAtDesc_WithValidUserIdAndStatus_ReturnsFilteredOrders() {
        // When
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(testUserId1, OrderStatus.PENDING);

        // Then
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0).getId()).isEqualTo(order1.getId());
        assertThat(pendingOrders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByUserIdAndStatusOrderByCreatedAtDesc_WithStatusNotFound_ReturnsEmptyList() {
        // When
        List<Order> canceledOrders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(testUserId1, OrderStatus.CANCELED);

        // Then
        assertThat(canceledOrders).isEmpty();
    }

    @Test
    void save_WithNewOrder_PersistsSuccessfully() {
        // Given
        Order newOrder = createTestOrder(UUID.randomUUID(), OrderStatus.PENDING, LocalDateTime.now());

        // When
        Order savedOrder = orderRepository.save(newOrder);

        // Then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getCreatedAt()).isNotNull();
        assertThat(savedOrder.getUpdatedAt()).isNotNull();

        // Verify it can be retrieved
        Optional<Order> retrievedOrder = orderRepository.findById(savedOrder.getId());
        assertThat(retrievedOrder).isPresent();
        assertThat(retrievedOrder.get().getUserId()).isEqualTo(newOrder.getUserId());
    }

    @Test
    void save_WithUpdatedOrder_UpdatesSuccessfully() {
        // Given
        Order existingOrder = orderRepository.findById(order1.getId()).orElseThrow();
        OrderStatus originalStatus = existingOrder.getStatus();
        LocalDateTime originalUpdatedAt = existingOrder.getUpdatedAt();

        // When
        existingOrder.setStatus(OrderStatus.CONFIRMED);
        existingOrder.setTotalAmount(BigDecimal.valueOf(200.00));
        Order savedOrder = orderRepository.save(existingOrder);

        // Then
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(savedOrder.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(savedOrder.getCreatedAt()).isEqualTo(existingOrder.getCreatedAt()); // Should not change
    }

    @Test
    void findById_WithExistingId_ReturnsOrder() {
        // When
        Optional<Order> foundOrder = orderRepository.findById(order1.getId());

        // Then
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getUserId()).isEqualTo(testUserId1);
        assertThat(foundOrder.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findById_WithNonExistentId_ReturnsEmpty() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<Order> foundOrder = orderRepository.findById(nonExistentId);

        // Then
        assertThat(foundOrder).isEmpty();
    }

    @Test
    void delete_WithExistingOrder_RemovesSuccessfully() {
        // Given
        UUID orderIdToDelete = order1.getId();

        // When
        orderRepository.delete(order1);
        entityManager.flush();

        // Then
        Optional<Order> deletedOrder = orderRepository.findById(orderIdToDelete);
        assertThat(deletedOrder).isEmpty();
    }

    @Test
    void existsById_WithExistingId_ReturnsTrue() {
        // When
        boolean exists = orderRepository.existsById(order1.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WithNonExistentId_ReturnsFalse() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        boolean exists = orderRepository.existsById(nonExistentId);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void findAll_ReturnsAllOrders() {
        // When
        List<Order> allOrders = orderRepository.findAll();

        // Then
        assertThat(allOrders).hasSize(3);
        assertThat(allOrders).extracting(Order::getId)
                .containsExactlyInAnyOrder(order1.getId(), order2.getId(), order3.getId());
    }

    @Test
    void count_ReturnsCorrectCount() {
        // When
        long count = orderRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void saveAndFlush_WithNewOrder_PersistsImmediately() {
        // Given
        Order newOrder = createTestOrder(UUID.randomUUID(), OrderStatus.PENDING, LocalDateTime.now());

        // When
        Order savedOrder = orderRepository.saveAndFlush(newOrder);

        // Then
        assertThat(savedOrder.getId()).isNotNull();

        // Verify it's immediately available in a new transaction
        entityManager.clear();
        Optional<Order> retrievedOrder = orderRepository.findById(savedOrder.getId());
        assertThat(retrievedOrder).isPresent();
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_WithMultipleStatuses_ReturnsAllOrdersForUser() {
        // Given - User1 has orders with different statuses
        Order additionalOrder = createTestOrder(testUserId1, OrderStatus.CANCELED, LocalDateTime.now());
        entityManager.persistAndFlush(additionalOrder);

        // When
        List<Order> allUserOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId1);

        // Then
        assertThat(allUserOrders).hasSize(3);
        assertThat(allUserOrders).extracting(Order::getStatus)
                .containsExactlyInAnyOrder(OrderStatus.CANCELED, OrderStatus.SHIPPED, OrderStatus.PENDING);
    }

    @Test
    void findByUserIdAndStatusOrderByCreatedAtDesc_WithMultipleOrdersSameStatus_ReturnsInCorrectOrder() {
        // Given - Create multiple orders with same status for the same user
        Order recentPendingOrder = createTestOrder(testUserId1, OrderStatus.PENDING, LocalDateTime.now());
        entityManager.persistAndFlush(recentPendingOrder);

        // When
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(testUserId1, OrderStatus.PENDING);

        // Then
        assertThat(pendingOrders).hasSize(2);
        assertThat(pendingOrders.get(0).getId()).isEqualTo(recentPendingOrder.getId()); // Most recent first
        assertThat(pendingOrders.get(1).getId()).isEqualTo(order1.getId());
        assertThat(pendingOrders.get(0).getCreatedAt()).isAfter(pendingOrders.get(1).getCreatedAt());
    }

    private Order createTestOrder(UUID userId, OrderStatus status, LocalDateTime createdAt) {
        Order order = new Order();
        order.setUserId(userId);
        order.setCartId(UUID.randomUUID());
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        order.setTax(BigDecimal.valueOf(10.00));
        order.setShippingCost(BigDecimal.valueOf(5.00));
        order.setDiscount(BigDecimal.valueOf(0.00));
        order.setBillingAddressId(UUID.randomUUID());
        order.setShippingAddressId(UUID.randomUUID());
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(createdAt);

        // Initialize discount fields
        order.setProductDiscount(BigDecimal.ZERO);
        order.setOrderLevelDiscount(BigDecimal.ZERO);
        order.setLoyaltyCouponDiscount(BigDecimal.ZERO);
        order.setTierBenefitDiscount(BigDecimal.ZERO);

        return order;
    }
}