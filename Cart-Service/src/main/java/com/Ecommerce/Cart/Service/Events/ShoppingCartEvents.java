package com.Ecommerce.Cart.Service.Events;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ShoppingCartEvents {

    /**
     * Base event for all shopping cart events
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class ShoppingCartEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private UUID userId;
        private UUID sessionId;

        public ShoppingCartEvent(String eventType) {
            this.eventId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
        }
    }

    /**
     * Event fired when a shopping cart is created
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartCreatedEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public CartCreatedEvent(ShoppingCart cart) {
            super("CART_CREATED");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.createdAt = cart.getCreatedAt();
            this.expiresAt = cart.getExpiresAt();
        }
    }

    /**
     * Event fired when a shopping cart is updated
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartUpdatedEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private int itemCount;
        private BigDecimal totalAmount;
        private LocalDateTime updatedAt;

        public CartUpdatedEvent(ShoppingCart cart) {
            super("CART_UPDATED");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.itemCount = cart.getItems().size();
            this.totalAmount = cart.calculateTotal();
            this.updatedAt = cart.getUpdatedAt();
        }
    }

    /**
     * Event fired when a shopping cart is deleted
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartDeletedEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private String deletionReason;
        private LocalDateTime deletedAt;

        public CartDeletedEvent(ShoppingCart cart, String deletionReason) {
            super("CART_DELETED");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.deletionReason = deletionReason;
            this.deletedAt = LocalDateTime.now();
        }
    }

    /**
     * Event fired when a shopping cart is abandoned
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartAbandonedEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private int itemCount;
        private List<CartItem> items;
        private BigDecimal totalAmount;
        private LocalDateTime lastActiveAt;
        private LocalDateTime abandonedAt;

        public CartAbandonedEvent(ShoppingCart cart, LocalDateTime abandonedAt) {
            super("CART_ABANDONED");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.itemCount = cart.getItems().size();
            this.items = cart.getItems();
            this.totalAmount = cart.calculateTotal();
            this.lastActiveAt = cart.getUpdatedAt();
            this.abandonedAt = abandonedAt;
        }
    }

    /**
     * Event fired when an abandoned shopping cart is recovered
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartRecoveredEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private LocalDateTime abandonedAt;
        private LocalDateTime recoveredAt;
        private String recoveryMethod; // EMAIL, NOTIFICATION, etc.

        public CartRecoveredEvent(ShoppingCart cart, LocalDateTime abandonedAt, String recoveryMethod) {
            super("CART_RECOVERED");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.abandonedAt = abandonedAt;
            this.recoveredAt = LocalDateTime.now();
            this.recoveryMethod = recoveryMethod;
        }
    }

    /**
     * Event fired when a shopping cart is checked out
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartCheckedOutEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private UUID orderId;
        private int itemCount;
        private List<CartItem> items;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal shipping;
        private BigDecimal discount;
        private BigDecimal totalAmount;
        private String paymentMethod;
        private LocalDateTime checkedOutAt;

        public CartCheckedOutEvent(ShoppingCart cart, UUID orderId, BigDecimal tax,
                                   BigDecimal shipping, BigDecimal discount, String paymentMethod) {
            super("CART_CHECKED_OUT");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.orderId = orderId;
            this.itemCount = cart.getItems().size();
            this.items = cart.getItems();
            this.subtotal = cart.calculateTotal();
            this.tax = tax;
            this.shipping = shipping;
            this.discount = discount;
            this.totalAmount = subtotal.add(tax).add(shipping).subtract(discount);
            this.paymentMethod = paymentMethod;
            this.checkedOutAt = LocalDateTime.now();
        }
    }

    /**
     * Event fired when a coupon is applied to a cart
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponAppliedEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private String couponCode;
        private BigDecimal discountAmount;
        private BigDecimal originalTotal;
        private BigDecimal discountedTotal;
        private LocalDateTime appliedAt;

        public CouponAppliedEvent(ShoppingCart cart, String couponCode,
                                  BigDecimal discountAmount, BigDecimal originalTotal) {
            super("COUPON_APPLIED");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.couponCode = couponCode;
            this.discountAmount = discountAmount;
            this.originalTotal = originalTotal;
            this.discountedTotal = originalTotal.subtract(discountAmount);
            this.appliedAt = LocalDateTime.now();
        }
    }

    /**
     * Event fired when a coupon is removed from a cart
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponRemovedEvent extends ShoppingCartEvent {
        private UUID cartId;
        private UUID userId;
        private String couponCode;
        private BigDecimal discountAmount;
        private LocalDateTime removedAt;

        public CouponRemovedEvent(ShoppingCart cart, String couponCode, BigDecimal discountAmount) {
            super("COUPON_REMOVED");
            this.cartId = cart.getId();
            this.userId = cart.getUserId();
            this.couponCode = couponCode;
            this.discountAmount = discountAmount;
            this.removedAt = LocalDateTime.now();
        }
    }
}