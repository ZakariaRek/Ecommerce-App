package com.Ecommerce.Cart.Service.Lisiteners;

import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Services.Kafka.SavedForLaterKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for SavedForLater documents to automatically publish events to Kafka
 * when items are saved for later, moved to cart, or removed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavedForLaterMongoListener extends AbstractMongoEventListener<SavedForLater> {

    private final SavedForLaterKafkaService kafkaService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<SavedForLater> event) {
        SavedForLater savedItem = event.getSource();
        String key = getEntityKey(savedItem);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update - though SavedForLater items aren't typically updated
                // They're usually just created or removed
                handleSavedItemUpdate(savedItem, oldState);
            } else {
                // This is a new saved item
                handleSavedItemCreation(savedItem);
            }
        } catch (Exception e) {
            log.error("Error in saved-for-later MongoDB listener after save", e);
        }
    }

    /**
     * Called before a document is deleted
     */
    @Override
    public void onBeforeDelete(BeforeDeleteEvent<SavedForLater> event) {
        try {
            // We don't have direct access to the document here, just the id
            // The document id is in the event's DBObject as _id
            Object id = event.getSource().get("_id");
            log.debug("Preparing to delete saved-for-later item with id: {}", id);

            // We can't access the actual SavedForLater here
            // The service layer should handle publishing removal events
        } catch (Exception e) {
            log.error("Error in saved-for-later MongoDB listener before delete", e);
        }
    }

    /**
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<SavedForLater> event) {
        try {
            // Similar to onBeforeDelete, we only have access to the document id
            Object id = event.getSource().get("_id");
            log.debug("Saved-for-later item with id: {} was deleted", id);
        } catch (Exception e) {
            log.error("Error in saved-for-later MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new saved-for-later item
     */
    private void handleSavedItemCreation(SavedForLater savedItem) {
        // Product name would typically come from a product service
        // For this example, we're setting it to null
        String productName = null;

        // Source cart ID may be null if saved directly from product page
        UUID sourceCartId = null;
        String source = "PRODUCT_PAGE";

        kafkaService.publishItemSavedForLater(savedItem, productName, sourceCartId, source);
        log.debug("MongoDB listener triggered for saved-for-later item creation: {}", savedItem.getId());
    }

    /**
     * Handle updates to an existing saved-for-later item
     * (These are rare, but included for completeness)
     */
    private void handleSavedItemUpdate(SavedForLater savedItem, EntityState oldState) {
        // No specific update events for SavedForLater as of now
        log.debug("MongoDB listener triggered for saved-for-later item update: {}", savedItem.getId());
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(SavedForLater savedItem) {
        String key = getEntityKey(savedItem);
        entityStateMap.put(key, new EntityState(savedItem.getSavedAt()));
        log.debug("Stored state before saved-for-later save: {}", savedItem.getId());
    }

    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(SavedForLater savedItem) {
        return "SavedForLater:" + savedItem.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final LocalDateTime savedAt;

        public EntityState(LocalDateTime savedAt) {
            this.savedAt = savedAt;
        }
    }
}