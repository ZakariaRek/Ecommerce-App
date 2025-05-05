package com.Ecommerce.Order_Service.Events;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderEvents {

    /**
     * Base event for all order events
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class OrderEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private UUID orderId;
        private UUID userId;

        public OrderEvent(String eventType) {
            this.eventId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
        }
    }

    /**
     * Event fired when an order is created
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreatedEvent extends OrderEvent {
        private UUID orderId;
        private UUID userId;
        private UUID cartId;
        private BigDecimal totalAmount;
        private BigDecimal tax;
        private BigDecimal shippingCost;
        private BigDecimal discount;
        private OrderStatus status;
        private LocalDateTime createdAt;

        public OrderCreatedEvent(Order order) {
            super("ORDER_CREATED");
            this.orderId = order.getId();
            this.userId = order.getUserId();
            this.cartId = order.getCartId();
            this.totalAmount = order.getTotalAmount();
            this.tax = order.getTax();
            this.shippingCost = order.getShippingCost();
            this.discount = order.getDiscount();
            this.status = order.getStatus();
            this.createdAt = order.getCreatedAt();
        }
    }

    /**
     * Event fired when an order status is changed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusChangedEvent extends OrderEvent {
        private UUID orderId;
        private UUID userId;
        private OrderStatus oldStatus;
        private OrderStatus newStatus;
        private LocalDateTime updatedAt;

        public OrderStatusChangedEvent(Order order, OrderStatus oldStatus) {
            super("ORDER_STATUS_CHANGED");
            this.orderId = order.getId();
            this.userId = order.getUserId();
            this.oldStatus = oldStatus;
            this.newStatus = order.getStatus();
            this.updatedAt = order.getUpdatedAt();
        }
    }

    /**
     * Event fired when an order is canceled
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCanceledEvent extends OrderEvent {
        private UUID orderId;
        private UUID userId;
        private OrderStatus previousStatus;
        private LocalDateTime canceledAt;
        private List<OrderItem> items;
        private BigDecimal refundAmount;

        public OrderCanceledEvent(Order order, OrderStatus previousStatus) {
            super("ORDER_CANCELED");
            this.orderId = order.getId();
            this.userId = order.getUserId();
            this.previousStatus = previousStatus;
            this.canceledAt = order.getUpdatedAt();
            this.items = order.getItems() != null ? List.copyOf(order.getItems()) : List.of();
            this.refundAmount = order.getTotalAmount();
        }
    }

    /**
     * Event fired when an order item is added
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemAddedEvent extends OrderEvent {
        private UUID orderId;
        private UUID userId;
        private UUID orderItemId;
        private UUID productId;
        private int quantity;
        private BigDecimal priceAtPurchase;
        private BigDecimal discount;
        private BigDecimal total;

        public OrderItemAddedEvent(Order order, OrderItem item) {
            super("ORDER_ITEM_ADDED");
            this.orderId = order.getId();
            this.userId = order.getUserId();
            this.orderItemId = item.getId();
            this.productId = item.getProductId();
            this.quantity = item.getQuantity();
            this.priceAtPurchase = item.getPriceAtPurchase();
            this.discount = item.getDiscount();
            this.total = item.getTotal();
        }
    }

    /**
     * Event fired when an order item is updated
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemUpdatedEvent extends OrderEvent {
        private UUID orderId;
        private UUID userId;
        private UUID orderItemId;
        private UUID productId;
        private int oldQuantity;
        private int newQuantity;
        private BigDecimal priceAtPurchase;
        private BigDecimal oldTotal;
        private BigDecimal newTotal;

        public OrderItemUpdatedEvent(Order order, OrderItem item, int oldQuantity) {
            super("ORDER_ITEM_UPDATED");
            this.orderId = order.getId();
            this.userId = order.getUserId();
            this.orderItemId = item.getId();
            this.productId = item.getProductId();
            this.oldQuantity = oldQuantity;
            this.newQuantity = item.getQuantity();
            this.priceAtPurchase = item.getPriceAtPurchase();
            // Calculate old total
            this.oldTotal = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(oldQuantity)).subtract(item.getDiscount());
            this.newTotal = item.getTotal();
        }
    }
}