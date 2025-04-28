package com.Ecommerce.Cart.Service.Events;

import com.Ecommerce.Cart.Service.Models.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CartItemEvents {

    /**
     * Base event for all cart item events
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class CartItemEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private UUID userId;
        private UUID sessionId;

        public CartItemEvent(String eventType) {
            this.eventId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
        }
    }

    /**
     * Event fired when a cart item is added
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemAddedEvent extends CartItemEvent {
        private UUID cartId;
        private UUID cartItemId;
        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal price;
        private BigDecimal subtotal;

        public CartItemAddedEvent(CartItem cartItem, String productName) {
            super("CART_ITEM_ADDED");
            this.cartId = cartItem.getCartId();
            this.cartItemId = cartItem.getId();
            this.productId = cartItem.getProductId();
            this.productName = productName;
            this.quantity = cartItem.getQuantity();
            this.price = cartItem.getPrice();
            this.subtotal = cartItem.getSubtotal();
        }
    }

    /**
     * Event fired when a cart item is updated
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemUpdatedEvent extends CartItemEvent {
        private UUID cartId;
        private UUID cartItemId;
        private UUID productId;
        private int oldQuantity;
        private int newQuantity;
        private BigDecimal oldPrice;
        private BigDecimal newPrice;
        private BigDecimal oldSubtotal;
        private BigDecimal newSubtotal;

        public CartItemUpdatedEvent(CartItem cartItem, int oldQuantity, BigDecimal oldPrice) {
            super("CART_ITEM_UPDATED");
            this.cartId = cartItem.getCartId();
            this.cartItemId = cartItem.getId();
            this.productId = cartItem.getProductId();
            this.oldQuantity = oldQuantity;
            this.newQuantity = cartItem.getQuantity();
            this.oldPrice = oldPrice;
            this.newPrice = cartItem.getPrice();
            this.oldSubtotal = oldPrice.multiply(BigDecimal.valueOf(oldQuantity));
            this.newSubtotal = cartItem.getSubtotal();
        }
    }

    /**
     * Event fired when a cart item is removed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemRemovedEvent extends CartItemEvent {
        private UUID cartId;
        private UUID cartItemId;
        private UUID productId;
        private int quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
        private String removalReason;

        public CartItemRemovedEvent(CartItem cartItem, String removalReason) {
            super("CART_ITEM_REMOVED");
            this.cartId = cartItem.getCartId();
            this.cartItemId = cartItem.getId();
            this.productId = cartItem.getProductId();
            this.quantity = cartItem.getQuantity();
            this.price = cartItem.getPrice();
            this.subtotal = cartItem.getSubtotal();
            this.removalReason = removalReason;
        }
    }

    /**
     * Event fired when a cart item quantity is changed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemQuantityChangedEvent extends CartItemEvent {
        private UUID cartId;
        private UUID cartItemId;
        private UUID productId;
        private int oldQuantity;
        private int newQuantity;
        private BigDecimal price;
        private BigDecimal oldSubtotal;
        private BigDecimal newSubtotal;

        public CartItemQuantityChangedEvent(CartItem cartItem, int oldQuantity) {
            super("CART_ITEM_QUANTITY_CHANGED");
            this.cartId = cartItem.getCartId();
            this.cartItemId = cartItem.getId();
            this.productId = cartItem.getProductId();
            this.oldQuantity = oldQuantity;
            this.newQuantity = cartItem.getQuantity();
            this.price = cartItem.getPrice();
            this.oldSubtotal = cartItem.getPrice().multiply(BigDecimal.valueOf(oldQuantity));
            this.newSubtotal = cartItem.getSubtotal();
        }
    }

    /**
     * Event fired when a cart item price is changed (e.g. due to product price update)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemPriceChangedEvent extends CartItemEvent {
        private UUID cartId;
        private UUID cartItemId;
        private UUID productId;
        private int quantity;
        private BigDecimal oldPrice;
        private BigDecimal newPrice;
        private BigDecimal oldSubtotal;
        private BigDecimal newSubtotal;
        private String reason;

        public CartItemPriceChangedEvent(CartItem cartItem, BigDecimal oldPrice, String reason) {
            super("CART_ITEM_PRICE_CHANGED");
            this.cartId = cartItem.getCartId();
            this.cartItemId = cartItem.getId();
            this.productId = cartItem.getProductId();
            this.quantity = cartItem.getQuantity();
            this.oldPrice = oldPrice;
            this.newPrice = cartItem.getPrice();
            this.oldSubtotal = oldPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            this.newSubtotal = cartItem.getSubtotal();
            this.reason = reason;
        }
    }

    /**
     * Event fired when a cart item goes out of stock
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemOutOfStockEvent extends CartItemEvent {
        private UUID cartId;
        private UUID cartItemId;
        private UUID productId;
        private int requestedQuantity;
        private int availableQuantity;

        public CartItemOutOfStockEvent(CartItem cartItem, int availableQuantity) {
            super("CART_ITEM_OUT_OF_STOCK");
            this.cartId = cartItem.getCartId();
            this.cartItemId = cartItem.getId();
            this.productId = cartItem.getProductId();
            this.requestedQuantity = cartItem.getQuantity();
            this.availableQuantity = availableQuantity;
        }
    }

    /**
     * Event fired when a product discount is applied to a cart item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDiscountAppliedEvent extends CartItemEvent {
        private UUID cartId;
        private UUID cartItemId;
        private UUID productId;
        private UUID discountId;
        private String discountType;
        private BigDecimal originalPrice;
        private BigDecimal discountedPrice;
        private BigDecimal discountAmount;
        private BigDecimal discountPercentage;

        public CartItemDiscountAppliedEvent(CartItem cartItem, UUID discountId, String discountType,
                                            BigDecimal originalPrice, BigDecimal discountAmount, BigDecimal discountPercentage) {
            super("CART_ITEM_DISCOUNT_APPLIED");
            this.cartId = cartItem.getCartId();
            this.cartItemId = cartItem.getId();
            this.productId = cartItem.getProductId();
            this.discountId = discountId;
            this.discountType = discountType;
            this.originalPrice = originalPrice;
            this.discountedPrice = cartItem.getPrice();
            this.discountAmount = discountAmount;
            this.discountPercentage = discountPercentage;
        }
    }
}