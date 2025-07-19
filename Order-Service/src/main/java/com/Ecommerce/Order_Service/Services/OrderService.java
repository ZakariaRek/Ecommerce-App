package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Repositories.OrderItemRepository;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import com.Ecommerce.Order_Service.Services.Kafka.OrderKafkaService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Service
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderKafkaService kafkaService;



    /**
     * Get all orders
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    /**
     * Creates a new order
     */
    public Order createOrder(String userId, UUID cartId, UUID billingAddressId, UUID shippingAddressId) {
        UUID userUuid;

        try {
            // Try to parse as UUID first
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // If not a UUID, assume it's a MongoDB ObjectId and convert it
            userUuid = convertObjectIdToUuid(userId);
        }
        Order order = Order.createOrder(userUuid, cartId, billingAddressId, shippingAddressId);
        Order savedOrder = orderRepository.save(order);
        // Publish event to Kafka
        kafkaService.publishOrderCreated(savedOrder);

        return savedOrder;
    }

    private UUID convertObjectIdToUuid(String objectId) {
        // Convert MongoDB ObjectId to UUID using a deterministic approach
        // This ensures the same ObjectId always maps to the same UUID
        try {
            // Use a hash-based approach to convert ObjectId to UUID
            byte[] bytes = objectId.getBytes(StandardCharsets.UTF_8);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(bytes);

            // Create UUID from hash bytes
            long mostSigBits = 0;
            long leastSigBits = 0;

            for (int i = 0; i < 8; i++) {
                mostSigBits = (mostSigBits << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                leastSigBits = (leastSigBits << 8) | (hash[i] & 0xff);
            }

            return new UUID(mostSigBits, leastSigBits);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid user ID format: " + objectId, e);
        }
    }

    /**
     * Get an order by ID
     */
    /**
     * Get an order by ID with items loaded
     */
    public Order getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Explicitly load items to avoid lazy loading issues
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        order.setItems(items);

        return order;
    }
    /**
     * Get all orders for a user
     */
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findRecentOrdersByUserId(userId);
    }

    /**
     * Updates the status of an order
     */
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        order.updateStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Publish event to Kafka
        kafkaService.publishOrderStatusChanged(updatedOrder, oldStatus);

        return updatedOrder;
    }

    /**
     * Cancels an order
     */
    public Order cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that has been shipped or delivered");
        }

        OrderStatus previousStatus = order.getStatus();
        order.cancelOrder();
        Order canceledOrder = orderRepository.save(order);

        // Publish event to Kafka
        kafkaService.publishOrderCanceled(canceledOrder, previousStatus);

        return canceledOrder;
    }

    /**
     * Generates an invoice for an order
     */
    public String generateInvoice(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        return order.generateInvoice();
    }

    /**
     * Get all items for an order
     */
    public List<OrderItem> getOrderItems(UUID orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new EntityNotFoundException("Order not found with ID: " + orderId);
        }
        return orderItemRepository.findByOrderId(orderId);
    }

    /**
     * Add an item to an order
     */
    public OrderItem addOrderItem(UUID orderId, OrderItem orderItem) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Check if order is in a state where items can be added
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Cannot add items to order in " + order.getStatus() + " status");
        }

        orderItem.setOrder(order);
        OrderItem savedItem = orderItemRepository.save(orderItem);

        // Recalculate order total
        BigDecimal newTotal = calculateOrderTotal(orderId);
        order.setTotalAmount(newTotal);
        orderRepository.save(order);

        // Publish event to Kafka
        kafkaService.publishOrderItemAdded(order, savedItem);

        return savedItem;
    }

    /**
     * Update an order item's quantity
     */
    public OrderItem updateOrderItemQuantity(UUID orderId, UUID itemId, int newQuantity) {
        // Verify order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Check if order is in a state where items can be modified
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Cannot modify items for order in " + order.getStatus() + " status");
        }

        // Find and update the item
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Order item not found with ID: " + itemId));

        // Verify item belongs to the specified order
        if (!item.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Item does not belong to the specified order");
        }

        // Store old quantity for event
        int oldQuantity = item.getQuantity();

        item.updateQuantity(newQuantity);
        OrderItem updatedItem = orderItemRepository.save(item);

        // Recalculate order total
        BigDecimal newTotal = calculateOrderTotal(orderId);
        order.setTotalAmount(newTotal);
        orderRepository.save(order);

        // Publish event to Kafka
        kafkaService.publishOrderItemUpdated(order, updatedItem, oldQuantity);

        return updatedItem;
    }

    /**
     * Calculates the total amount for an order based on its items
     */
    public BigDecimal calculateOrderTotal(UUID orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return items.stream()
                .map(OrderItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Remove an order item from an order
     */
    public void removeOrderItem(UUID orderId, UUID itemId) {
        // Verify order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Check if order is in a state where items can be removed
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Cannot remove items from order in " + order.getStatus() + " status");
        }

        // Find the item to remove
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Order item not found with ID: " + itemId));

        // Verify item belongs to the specified order
        if (!item.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Item does not belong to the specified order");
        }

        // Remove the item
        orderItemRepository.delete(item);

        // Recalculate order total
        BigDecimal newTotal = calculateOrderTotal(orderId);
        order.setTotalAmount(newTotal);
        orderRepository.save(order);

        // Publish event to Kafka
        kafkaService.publishOrderItemUpdated(order, item, item.getQuantity());
    }
}