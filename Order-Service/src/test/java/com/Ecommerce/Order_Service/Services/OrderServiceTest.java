package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.KafkaProducers.OrderKafkaService;
import com.Ecommerce.Order_Service.Repositories.OrderItemRepository;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderKafkaService kafkaService;

    @InjectMocks
    private OrderService orderService;

    private UUID testOrderId;
    private UUID testUserId;
    private UUID testCartId;
    private UUID testBillingAddressId;
    private UUID testShippingAddressId;
    private UUID testItemId;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCartId = UUID.randomUUID();
        testBillingAddressId = UUID.randomUUID();
        testShippingAddressId = UUID.randomUUID();
        testItemId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setUserId(testUserId);
        testOrder.setCartId(testCartId);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setTax(BigDecimal.valueOf(10.00));
        testOrder.setShippingCost(BigDecimal.valueOf(5.00));
        testOrder.setDiscount(BigDecimal.ZERO);
        testOrder.setBillingAddressId(testBillingAddressId);
        testOrder.setShippingAddressId(testShippingAddressId);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setId(testItemId);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProductId(UUID.randomUUID());
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(29.99));
        testOrderItem.setDiscount(BigDecimal.ZERO);
    }

    @Test
    void getAllOrders_ReturnsAllOrders() {
        // Given
        List<Order> expectedOrders = List.of(testOrder);
        when(orderRepository.findAll()).thenReturn(expectedOrders);

        // When
        List<Order> actualOrders = orderService.getAllOrders();

        // Then
        assertThat(actualOrders).hasSize(1);
        assertThat(actualOrders.get(0)).isEqualTo(testOrder);
        verify(orderRepository).findAll();
    }

    @Test
    void createOrder_WithValidUUID_CreatesOrderSuccessfully() {
        // Given
        String userIdString = testUserId.toString();
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order createdOrder = orderService.createOrder(userIdString, testCartId, testBillingAddressId, testShippingAddressId);

        // Then
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getId()).isEqualTo(testOrderId);
        assertThat(createdOrder.getUserId()).isEqualTo(testUserId);
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);

        verify(orderRepository).save(any(Order.class));
        verify(kafkaService).publishOrderCreated(testOrder);
    }

    @Test
    void createOrder_WithInvalidUUID_ConvertsObjectIdToUUID() {
        // Given
        String objectIdString = "507f1f77bcf86cd799439011";
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order createdOrder = orderService.createOrder(objectIdString, testCartId, testBillingAddressId, testShippingAddressId);

        // Then
        assertThat(createdOrder).isNotNull();
        verify(orderRepository).save(any(Order.class));
        verify(kafkaService).publishOrderCreated(any(Order.class));
    }

    @Test
    void createOrder_WithInvalidFormat_ThrowsException() {
        // Given
        String invalidUserId = "invalid-format";

        // When & Then
        assertThatThrownBy(() ->
                orderService.createOrder(invalidUserId, testCartId, testBillingAddressId, testShippingAddressId)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid user ID format");
    }

    @Test
    void getOrderById_WithValidId_ReturnsOrder() {
        // Given
        List<OrderItem> items = List.of(testOrderItem);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findByOrderId(testOrderId)).thenReturn(items);

        // When
        Order foundOrder = orderService.getOrderById(testOrderId);

        // Then
        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getId()).isEqualTo(testOrderId);
        assertThat(foundOrder.getItems()).hasSize(1);

        verify(orderRepository).findById(testOrderId);
        verify(orderItemRepository).findByOrderId(testOrderId);
    }

    @Test
    void getOrderById_WithInvalidId_ThrowsEntityNotFoundException() {
        // Given
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(testOrderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found with ID");

        verify(orderRepository).findById(testOrderId);
    }

    @Test
    void getOrdersByUserId_ReturnsUserOrders() {
        // Given
        List<Order> expectedOrders = List.of(testOrder);
        when(orderRepository.findRecentOrdersByUserId(testUserId)).thenReturn(expectedOrders);

        // When
        List<Order> actualOrders = orderService.getOrdersByUserId(testUserId);

        // Then
        assertThat(actualOrders).hasSize(1);
        assertThat(actualOrders.get(0)).isEqualTo(testOrder);
        verify(orderRepository).findRecentOrdersByUserId(testUserId);
    }

    @Test
    void updateOrderStatus_WithValidOrder_UpdatesSuccessfully() {
        // Given
        OrderStatus newStatus = OrderStatus.SHIPPED;
        OrderStatus oldStatus = testOrder.getStatus();
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order updatedOrder = orderService.updateOrderStatus(testOrderId, newStatus);

        // Then
        assertThat(updatedOrder.getStatus()).isEqualTo(newStatus);
        verify(orderRepository).findById(testOrderId);
        verify(orderRepository).save(testOrder);
        verify(kafkaService).publishOrderStatusChanged(testOrder, oldStatus);
    }

    @Test
    void updateOrderStatus_WithInvalidOrderId_ThrowsEntityNotFoundException() {
        // Given
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(testOrderId, OrderStatus.SHIPPED))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository).findById(testOrderId);
        verifyNoMoreInteractions(orderRepository);
        verifyNoInteractions(kafkaService);
    }

    @Test
    void cancelOrder_WithValidPendingOrder_CancelsSuccessfully() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        OrderStatus previousStatus = testOrder.getStatus();
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order canceledOrder = orderService.cancelOrder(testOrderId);

        // Then
        assertThat(canceledOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(orderRepository).findById(testOrderId);
        verify(orderRepository).save(testOrder);
        verify(kafkaService).publishOrderCanceled(testOrder, previousStatus);
    }

    @Test
    void cancelOrder_WithShippedOrder_ThrowsIllegalStateException() {
        // Given
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(testOrderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an order that has been shipped or delivered");

        verify(orderRepository).findById(testOrderId);
        verifyNoMoreInteractions(orderRepository);
        verifyNoInteractions(kafkaService);
    }

    @Test
    void cancelOrder_WithDeliveredOrder_ThrowsIllegalStateException() {
        // Given
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(testOrderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an order that has been shipped or delivered");

        verify(orderRepository).findById(testOrderId);
        verifyNoMoreInteractions(orderRepository);
        verifyNoInteractions(kafkaService);
    }

    @Test
    void generateInvoice_WithValidOrder_GeneratesInvoice() {
        // Given
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

        // When
        String invoice = orderService.generateInvoice(testOrderId);

        // Then
        assertThat(invoice).contains("Invoice for Order " + testOrderId);
        verify(orderRepository).findById(testOrderId);
    }

    @Test
    void generateInvoice_WithInvalidOrderId_ThrowsEntityNotFoundException() {
        // Given
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.generateInvoice(testOrderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository).findById(testOrderId);
    }

    @Test
    void getOrderItems_WithValidOrderId_ReturnsItems() {
        // Given
        List<OrderItem> expectedItems = List.of(testOrderItem);
        when(orderRepository.existsById(testOrderId)).thenReturn(true);
        when(orderItemRepository.findByOrderId(testOrderId)).thenReturn(expectedItems);

        // When
        List<OrderItem> actualItems = orderService.getOrderItems(testOrderId);

        // Then
        assertThat(actualItems).hasSize(1);
        assertThat(actualItems.get(0)).isEqualTo(testOrderItem);
        verify(orderRepository).existsById(testOrderId);
        verify(orderItemRepository).findByOrderId(testOrderId);
    }

    @Test
    void getOrderItems_WithInvalidOrderId_ThrowsEntityNotFoundException() {
        // Given
        when(orderRepository.existsById(testOrderId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderItems(testOrderId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found with ID");

        verify(orderRepository).existsById(testOrderId);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void addOrderItem_ToPendingOrder_AddsSuccessfully() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setItems(new ArrayList<>());

        List<OrderItem> existingItems = List.of();
        List<OrderItem> updatedItems = List.of(testOrderItem);

        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.save(testOrderItem)).thenReturn(testOrderItem);
        when(orderItemRepository.findByOrderId(testOrderId))
                .thenReturn(existingItems)
                .thenReturn(updatedItems);
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When
        OrderItem addedItem = orderService.addOrderItem(testOrderId, testOrderItem);

        // Then
        assertThat(addedItem).isNotNull();
        assertThat(addedItem.getOrder()).isEqualTo(testOrder);
        verify(orderRepository).findById(testOrderId);
        verify(orderItemRepository).save(testOrderItem);
        verify(kafkaService).publishOrderItemAdded(testOrder, testOrderItem);
    }

    @Test
    void addOrderItem_ToShippedOrder_ThrowsIllegalStateException() {
        // Given
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.addOrderItem(testOrderId, testOrderItem))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot add items to order in SHIPPED status");

        verify(orderRepository).findById(testOrderId);
        verifyNoInteractions(orderItemRepository);
        verifyNoInteractions(kafkaService);
    }

    @Test
    void updateOrderItemQuantity_WithValidItem_UpdatesSuccessfully() {
        // Given
        int oldQuantity = testOrderItem.getQuantity();
        int newQuantity = 5;
        testOrder.setStatus(OrderStatus.PENDING);
        testOrderItem.setOrder(testOrder);

        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findById(testItemId)).thenReturn(Optional.of(testOrderItem));
        when(orderItemRepository.save(testOrderItem)).thenReturn(testOrderItem);
        when(orderItemRepository.findByOrderId(testOrderId)).thenReturn(List.of(testOrderItem));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When
        OrderItem updatedItem = orderService.updateOrderItemQuantity(testOrderId, testItemId, newQuantity);

        // Then
        assertThat(updatedItem.getQuantity()).isEqualTo(newQuantity);
        verify(orderRepository).findById(testOrderId);
        verify(orderItemRepository).findById(testItemId);
        verify(orderItemRepository).save(testOrderItem);
        verify(kafkaService).publishOrderItemUpdated(testOrder, testOrderItem, oldQuantity);
    }

    @Test
    void updateOrderItemQuantity_WithNonExistentItem_ThrowsEntityNotFoundException() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findById(testItemId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderItemQuantity(testOrderId, testItemId, 5))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order item not found");

        verify(orderRepository).findById(testOrderId);
        verify(orderItemRepository).findById(testItemId);
        verifyNoInteractions(kafkaService);
    }

    @Test
    void updateOrderItemQuantity_ItemBelongsToDifferentOrder_ThrowsIllegalArgumentException() {
        // Given
        UUID differentOrderId = UUID.randomUUID();
        Order differentOrder = new Order();
        differentOrder.setId(differentOrderId);
        testOrderItem.setOrder(differentOrder);

        testOrder.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findById(testItemId)).thenReturn(Optional.of(testOrderItem));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderItemQuantity(testOrderId, testItemId, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item does not belong to the specified order");

        verify(orderRepository).findById(testOrderId);
        verify(orderItemRepository).findById(testItemId);
        verifyNoInteractions(kafkaService);
    }

    @Test
    void calculateOrderTotal_WithItems_ReturnsCorrectTotal() {
        // Given
        OrderItem item1 = new OrderItem();
        item1.setPriceAtPurchase(BigDecimal.valueOf(20.00));
        item1.setQuantity(2);
        item1.setDiscount(BigDecimal.valueOf(5.00));

        OrderItem item2 = new OrderItem();
        item2.setPriceAtPurchase(BigDecimal.valueOf(30.00));
        item2.setQuantity(1);
        item2.setDiscount(BigDecimal.ZERO);

        List<OrderItem> items = List.of(item1, item2);
        when(orderItemRepository.findByOrderId(testOrderId)).thenReturn(items);

        // When
        BigDecimal total = orderService.calculateOrderTotal(testOrderId);

        // Then
        // item1 total: (20.00 * 2) - 5.00 = 35.00
        // item2 total: (30.00 * 1) - 0.00 = 30.00
        // Total: 35.00 + 30.00 = 65.00
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(65.00));
        verify(orderItemRepository).findByOrderId(testOrderId);
    }

    @Test
    void calculateOrderTotal_WithNoItems_ReturnsZero() {
        // Given
        when(orderItemRepository.findByOrderId(testOrderId)).thenReturn(Collections.emptyList());

        // When
        BigDecimal total = orderService.calculateOrderTotal(testOrderId);

        // Then
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        verify(orderItemRepository).findByOrderId(testOrderId);
    }

    @Test
    void removeOrderItem_FromPendingOrder_RemovesSuccessfully() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        testOrderItem.setOrder(testOrder);

        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderItemRepository.findById(testItemId)).thenReturn(Optional.of(testOrderItem));
        when(orderItemRepository.findByOrderId(testOrderId)).thenReturn(Collections.emptyList());
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        // When
        orderService.removeOrderItem(testOrderId, testItemId);

        // Then
        verify(orderRepository).findById(testOrderId);
        verify(orderItemRepository).findById(testItemId);
        verify(orderItemRepository).delete(testOrderItem);
        verify(kafkaService).publishOrderItemUpdated(testOrder, testOrderItem, testOrderItem.getQuantity());
    }

    @Test
    void removeOrderItem_FromCompletedOrder_ThrowsIllegalStateException() {
        // Given
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.removeOrderItem(testOrderId, testItemId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot remove items from order in COMPLETED status");

        verify(orderRepository).findById(testOrderId);
        verifyNoInteractions(orderItemRepository);
        verifyNoInteractions(kafkaService);
    }
}