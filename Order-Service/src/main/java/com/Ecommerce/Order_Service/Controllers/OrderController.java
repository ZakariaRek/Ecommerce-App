package com.Ecommerce.Order_Service.Controllers;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for order operations
 */
@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Create a new order
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            UUID userId = UUID.fromString((String) orderRequest.get("userId"));
            UUID cartId = UUID.fromString((String) orderRequest.get("cartId"));
            UUID billingAddressId = UUID.fromString((String) orderRequest.get("billingAddressId"));
            UUID shippingAddressId = UUID.fromString((String) orderRequest.get("shippingAddressId"));

            Order newOrder = orderService.createOrder(userId, cartId, billingAddressId, shippingAddressId);
            return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID format: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating order: " + e.getMessage());
        }
    }

    /**
     * Get an order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Get all orders for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable UUID userId) {
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Update order status
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> statusUpdate) {
        try {
            OrderStatus newStatus = OrderStatus.valueOf(statusUpdate.get("status"));
            Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Cancel an order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable UUID orderId) {
        try {
            Order canceledOrder = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(canceledOrder);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Generate invoice for an order
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<String> generateInvoice(@PathVariable UUID orderId) {
        try {
            String invoice = orderService.generateInvoice(orderId);
            return ResponseEntity.ok(invoice);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Get order items for an order
     */
    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItem>> getOrderItems(@PathVariable UUID orderId) {
        try {
            List<OrderItem> items = orderService.getOrderItems(orderId);
            return ResponseEntity.ok(items);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Add item to an order
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItem> addOrderItem(
            @PathVariable UUID orderId,
            @RequestBody OrderItem orderItem) {
        try {
            OrderItem addedItem = orderService.addOrderItem(orderId, orderItem);
            return new ResponseEntity<>(addedItem, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Update item quantity
     */
    @PatchMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderItem> updateItemQuantity(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId,
            @RequestBody Map<String, Integer> quantityUpdate) {
        try {
            int newQuantity = quantityUpdate.get("quantity");
            OrderItem updatedItem = orderService.updateOrderItemQuantity(orderId, itemId, newQuantity);
            return ResponseEntity.ok(updatedItem);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Calculate order total
     */
    @GetMapping("/{orderId}/total")
    public ResponseEntity<Map<String, BigDecimal>> calculateOrderTotal(@PathVariable UUID orderId) {
        try {
            BigDecimal total = orderService.calculateOrderTotal(orderId);
            return ResponseEntity.ok(Map.of("total", total));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}