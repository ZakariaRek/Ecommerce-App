package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Repositories.OrderItemRepository;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /**
     * Creates a new order
     */
    public Order createOrder(UUID userId, UUID cartId, UUID billingAddressId, UUID shippingAddressId) {
        Order order = Order.createOrder(userId, cartId, billingAddressId, shippingAddressId);
        return orderRepository.save(order);
    }

    /**
     * Get an order by ID
     */
    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));
    }

    /**
     * Get all orders for a user
     */
    public List<Order> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * Updates the status of an order
     */
    public Order updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        order.updateStatus(newStatus);
        return orderRepository.save(order);
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
        order.cancelOrder();
        return orderRepository.save(order);
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

        item.updateQuantity(newQuantity);
        OrderItem updatedItem = orderItemRepository.save(item);

        // Recalculate order total
        BigDecimal newTotal = calculateOrderTotal(orderId);
        order.setTotalAmount(newTotal);
        orderRepository.save(order);

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
}