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


        public ItemSavedForLaterEvent(SavedForLater savedItem) {
            super("ITEM_SAVED_FOR_LATER");
            this.savedItemId = savedItem.getId();
            this.userId = savedItem.getUserId();
            this.productId = savedItem.getProductId();

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