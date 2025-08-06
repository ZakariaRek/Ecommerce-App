package com.Ecommerce.Order_Service.KafkaProducers;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Events.OrderEvents;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderKafkaServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderKafkaService orderKafkaService;

    private UUID testOrderId;
    private UUID testUserId;
    private UUID testCartId;
    private UUID testItemId;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCartId = UUID.randomUUID();
        testItemId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setUserId(testUserId);
        testOrder.setCartId(testCartId);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setTax(BigDecimal.valueOf(10.00));
        testOrder.setShippingCost(BigDecimal.valueOf(5.00));
        testOrder.setDiscount(BigDecimal.valueOf(15.00));
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setId(testItemId);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProductId(UUID.randomUUID());
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(50.00));
        testOrderItem.setDiscount(BigDecimal.valueOf(10.00));

        testOrder.setItems(List.of(testOrderItem));
    }

    @Test
    void publishOrderCreated_WithValidOrder_PublishesEventSuccessfully() {
        // When
        orderKafkaService.publishOrderCreated(testOrder);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvents.OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderCreatedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-created");
        assertThat(keyCaptor.getValue()).isEqualTo(testUserId.toString());

        OrderEvents.OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(testOrderId);
        assertThat(capturedEvent.getUserId()).isEqualTo(testUserId);
        assertThat(capturedEvent.getCartId()).isEqualTo(testCartId);
        assertThat(capturedEvent.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(capturedEvent.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void publishOrderCreated_WithKafkaException_HandlesGracefully() {
        // Given
        doThrow(new RuntimeException("Kafka connection failed"))
                .when(kafkaTemplate).send(any(), any(), any());

        // When & Then - Should not throw exception
        assertThatCode(() -> orderKafkaService.publishOrderCreated(testOrder))
                .doesNotThrowAnyException();

        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void publishOrderStatusChanged_WithValidOrder_PublishesEventSuccessfully() {
        // Given
        OrderStatus oldStatus = OrderStatus.PENDING;
        testOrder.setStatus(OrderStatus.SHIPPED);

        // When
        orderKafkaService.publishOrderStatusChanged(testOrder, oldStatus);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvents.OrderStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderStatusChangedEvent.class);

        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        // First call should be for order-status-changed
        assertThat(topicCaptor.getAllValues().get(0)).isEqualTo("order-status-changed");
        assertThat(keyCaptor.getAllValues().get(0)).isEqualTo(testUserId.toString());

        OrderEvents.OrderStatusChangedEvent statusChangedEvent = eventCaptor.getAllValues().get(0);
        assertThat(statusChangedEvent.getOrderId()).isEqualTo(testOrderId);
        assertThat(statusChangedEvent.getUserId()).isEqualTo(testUserId);
        assertThat(statusChangedEvent.getOldStatus()).isEqualTo(oldStatus);
        assertThat(statusChangedEvent.getNewStatus()).isEqualTo(OrderStatus.SHIPPED);

        // Second call should be for order-completed (since SHIPPED is considered completed)
        assertThat(topicCaptor.getAllValues().get(1)).isEqualTo("order-completed");
    }

    @Test
    void publishOrderStatusChanged_FromPendingToCompleted_PublishesCompletedEvent() {
        // Given
        OrderStatus oldStatus = OrderStatus.PENDING;
        testOrder.setStatus(OrderStatus.DELIVERED);

        // Mock repository for first order check
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(List.of(testOrder)); // This is the only completed order

        // When
        orderKafkaService.publishOrderStatusChanged(testOrder, oldStatus);

        // Then - Should publish both status changed and completed events
        verify(kafkaTemplate, times(2)).send(any(), any(), any());
    }

    @Test
    void publishOrderStatusChanged_FromShippedToDelivered_DoesNotPublishCompletedAgain() {
        // Given
        OrderStatus oldStatus = OrderStatus.SHIPPED; // Already completed status
        testOrder.setStatus(OrderStatus.DELIVERED);

        // When
        orderKafkaService.publishOrderStatusChanged(testOrder, oldStatus);

        // Then - Should only publish status changed event, not completed again
        verify(kafkaTemplate, times(1)).send(any(), any(), any());
    }

    @Test
    void publishOrderCanceled_WithValidOrder_PublishesEventSuccessfully() {
        // Given
        OrderStatus previousStatus = OrderStatus.PENDING;

        // When
        orderKafkaService.publishOrderCanceled(testOrder, previousStatus);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvents.OrderCanceledEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderCanceledEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-canceled");
        assertThat(keyCaptor.getValue()).isEqualTo(testUserId.toString());

        OrderEvents.OrderCanceledEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(testOrderId);
        assertThat(capturedEvent.getUserId()).isEqualTo(testUserId);
        assertThat(capturedEvent.getPreviousStatus()).isEqualTo(previousStatus);
        assertThat(capturedEvent.getRefundAmount()).isEqualByComparingTo(testOrder.getTotalAmount());
        assertThat(capturedEvent.getItems()).hasSize(1);
    }

    @Test
    void publishOrderItemAdded_WithValidOrderItem_PublishesEventSuccessfully() {
        // When
        orderKafkaService.publishOrderItemAdded(testOrder, testOrderItem);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvents.OrderItemAddedEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderItemAddedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-item-added");
        assertThat(keyCaptor.getValue()).isEqualTo(testUserId.toString());

        OrderEvents.OrderItemAddedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(testOrderId);
        assertThat(capturedEvent.getUserId()).isEqualTo(testUserId);
        assertThat(capturedEvent.getOrderItemId()).isEqualTo(testItemId);
        assertThat(capturedEvent.getProductId()).isEqualTo(testOrderItem.getProductId());
        assertThat(capturedEvent.getQuantity()).isEqualTo(2);
        assertThat(capturedEvent.getPriceAtPurchase()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void publishOrderItemUpdated_WithValidOrderItem_PublishesEventSuccessfully() {
        // Given
        int oldQuantity = 1;

        // When
        orderKafkaService.publishOrderItemUpdated(testOrder, testOrderItem, oldQuantity);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvents.OrderItemUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderItemUpdatedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-item-updated");
        assertThat(keyCaptor.getValue()).isEqualTo(testUserId.toString());

        OrderEvents.OrderItemUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(testOrderId);
        assertThat(capturedEvent.getUserId()).isEqualTo(testUserId);
        assertThat(capturedEvent.getOrderItemId()).isEqualTo(testItemId);
        assertThat(capturedEvent.getOldQuantity()).isEqualTo(oldQuantity);
        assertThat(capturedEvent.getNewQuantity()).isEqualTo(2);

        // Verify total calculations
        // Old total: (50.00 * 1) - 10.00 = 40.00
        // New total: (50.00 * 2) - 10.00 = 90.00
        assertThat(capturedEvent.getOldTotal()).isEqualByComparingTo(BigDecimal.valueOf(40.00));
        assertThat(capturedEvent.getNewTotal()).isEqualByComparingTo(BigDecimal.valueOf(90.00));
    }

    @Test
    void publishOrderCompleted_WithFirstOrder_PublishesEventWithFirstOrderFlag() {
        // Given
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(List.of(testOrder)); // Only one order (the current one)

        // When
        orderKafkaService.publishOrderCompleted(testOrder);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderEvents.OrderCompletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderCompletedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("order-completed");
        assertThat(keyCaptor.getValue()).isEqualTo(testUserId.toString());

        OrderEvents.OrderCompletedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getOrderId()).isEqualTo(testOrderId);
        assertThat(capturedEvent.getUserId()).isEqualTo(testUserId);
        assertThat(capturedEvent.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(capturedEvent.getItemCount()).isEqualTo(1);
        assertThat(capturedEvent.isFirstOrder()).isTrue();
        assertThat(capturedEvent.getPaymentMethod()).isEqualTo("UNKNOWN");
        assertThat(capturedEvent.getOrderStatus()).isEqualTo(OrderStatus.PENDING.toString());
    }

    @Test
    void publishOrderCompleted_WithMultipleOrders_PublishesEventWithFirstOrderFalse() {
        // Given
        Order previousOrder = new Order();
        previousOrder.setId(UUID.randomUUID());
        previousOrder.setUserId(testUserId);
        previousOrder.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(List.of(testOrder, previousOrder)); // Multiple orders

        // When
        orderKafkaService.publishOrderCompleted(testOrder);

        // Then
        ArgumentCaptor<OrderEvents.OrderCompletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderCompletedEvent.class);
        verify(kafkaTemplate).send(any(), any(), eventCaptor.capture());

        OrderEvents.OrderCompletedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.isFirstOrder()).isFalse();
    }

    @Test
    void publishOrderCompleted_WithRepositoryException_HandlesGracefully() {
        // Given
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - Should not throw exception
        assertThatCode(() -> orderKafkaService.publishOrderCompleted(testOrder))
                .doesNotThrowAnyException();

        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void isOrderCompleted_WithCompletedStatuses_ReturnsTrue() {
        // Test all completed statuses
        testOrder.setStatus(OrderStatus.CONFIRMED);
        orderKafkaService.publishOrderStatusChanged(testOrder, OrderStatus.PENDING);
        verify(kafkaTemplate, times(2)).send(any(), any(), any()); // status change + completed

        reset(kafkaTemplate);

        testOrder.setStatus(OrderStatus.DELIVERED);
        orderKafkaService.publishOrderStatusChanged(testOrder, OrderStatus.SHIPPED);
        verify(kafkaTemplate, times(1)).send(any(), any(), any()); // only status change (already completed)

        reset(kafkaTemplate);

        testOrder.setStatus(OrderStatus.SHIPPED);
        orderKafkaService.publishOrderStatusChanged(testOrder, OrderStatus.PENDING);
        verify(kafkaTemplate, times(2)).send(any(), any(), any()); // status change + completed
    }

    @Test
    void isOrderCompleted_WithNonCompletedStatuses_ReturnsFalse() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);

        // When
        orderKafkaService.publishOrderStatusChanged(testOrder, OrderStatus.CANCELED);

        // Then - Should only publish status change, not completed
        verify(kafkaTemplate, times(1)).send(any(), any(), any());
    }

    @Test
    void isFirstCompletedOrder_WithMixOfStatusesIncludingCompleted_CountsCorrectly() {
        // Given
        Order pendingOrder = new Order();
        pendingOrder.setStatus(OrderStatus.PENDING);

        Order canceledOrder = new Order();
        canceledOrder.setStatus(OrderStatus.CANCELED);

        Order completedOrder = new Order();
        completedOrder.setStatus(OrderStatus.DELIVERED);

        testOrder.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(List.of(testOrder, pendingOrder, canceledOrder, completedOrder));

        // When
        orderKafkaService.publishOrderCompleted(testOrder);

        // Then
        ArgumentCaptor<OrderEvents.OrderCompletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvents.OrderCompletedEvent.class);
        verify(kafkaTemplate).send(any(), any(), eventCaptor.capture());

        // Should be false because there are 2 completed orders (testOrder + completedOrder)
        OrderEvents.OrderCompletedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.isFirstOrder()).isFalse();
    }
}