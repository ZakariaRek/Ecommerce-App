package com.Ecommerce.Cart.Service.Events;

import com.Ecommerce.Cart.Service.Models.SavedForLater;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class SavedForLaterEvents {

    /**
     * Base event for all saved-for-later events
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class SavedForLaterEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private UUID userId;
        private UUID sessionId;

        public SavedForLaterEvent(String eventType) {
            this.eventId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
        }
    }

    /**
     * Event fired when an item is saved for later
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemSavedForLaterEvent extends SavedForLaterEvent {
        private UUID savedItemId;
        private UUID userId;
        private UUID productId;
        private String productName;
        private UUID sourceCartId;
        private String source; // "CART", "PRODUCT_PAGE", etc.

        public ItemSavedForLaterEvent(SavedForLater savedItem, String productName, UUID sourceCartId, String source) {
            super("ITEM_SAVED_FOR_LATER");
            this.savedItemId = savedItem.getId();
            this.userId = savedItem.getUserId();
            this.productId = savedItem.getProductId();
            this.productName = productName;
            this.sourceCartId = sourceCartId;
            this.source = source;
        }
    }

    /**
     * Event fired when a saved item is moved to cart
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedItemMovedToCartEvent extends SavedForLaterEvent {
        private UUID savedItemId;
        private UUID userId;
        private UUID productId;
        private UUID cartId;
        private UUID newCartItemId;

        public SavedItemMovedToCartEvent(SavedForLater savedItem, UUID cartId, UUID newCartItemId) {
            super("SAVED_ITEM_MOVED_TO_CART");
            this.savedItemId = savedItem.getId();
            this.userId = savedItem.getUserId();
            this.productId = savedItem.getProductId();
            this.cartId = cartId;
            this.newCartItemId = newCartItemId;
        }
    }

    /**
     * Event fired when a saved item is removed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedItemRemovedEvent extends SavedForLaterEvent {
        private UUID savedItemId;
        private UUID userId;
        private UUID productId;
        private String removalReason;

        public SavedItemRemovedEvent(SavedForLater savedItem, String removalReason) {
            super("SAVED_ITEM_REMOVED");
            this.savedItemId = savedItem.getId();
            this.userId = savedItem.getUserId();
            this.productId = savedItem.getProductId();
            this.removalReason = removalReason;
        }
    }
}