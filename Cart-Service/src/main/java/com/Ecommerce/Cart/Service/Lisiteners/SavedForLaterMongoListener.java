package com.Ecommerce.Cart.Service.Lisiteners;

import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Services.Kafka.SavedForLaterKafkaService;
import com.Ecommerce.Cart.Service.Services.SavedForLaterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final SavedForLaterService savedForLaterService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    // Store items before deletion to use in after-delete events
    private static final Map<String, SavedForLater> preDeleteItemMap = new ConcurrentHashMap<>();

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
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<SavedForLater> event) {
        try {
            // Extract the ID from the deleted document
            Object id = event.getSource().get("_id");
            if (id != null) {
                String documentId = id.toString();
                log.debug("Saved-for-later item with id: {} was deleted", documentId);

                // Look for the stored SavedForLater item from the beforeDelete event
                String key = "SavedForLater:" + documentId;
                SavedForLater deletedItem = preDeleteItemMap.remove(key);

                if (deletedItem != null) {
                    // Determine the reason for removal (could be expanded with more context)
                    String removalReason = "user_deleted";

                    // Always publish the remove event with the appropriate reason
                    kafkaService.publishSavedItemRemoved(deletedItem, removalReason);
                    log.debug("Published Kafka event for removed saved-for-later item: {}", documentId);
                } else {
                    log.warn("Could not find pre-delete state for saved-for-later item with id: {}", documentId);
                }
            }
        } catch (Exception e) {
            log.error("Error in saved-for-later MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new saved-for-later item
     */
    private void handleSavedItemCreation(SavedForLater savedItem) {




        kafkaService.publishItemSavedForLater(savedItem);
        log.debug("MongoDB listener triggered for saved-for-later item creation: {}", savedItem.getId());
    }

    /**
     * Handle updates to an existing saved-for-later item
     * (These are rare, but included for completeness)
     */
    private void handleSavedItemUpdate(SavedForLater savedItem, EntityState oldState) {
        // Check for specific changes that might need events
        LocalDateTime oldSavedAt = oldState.savedAt;
        LocalDateTime newSavedAt = savedItem.getSavedAt();

        if (!oldSavedAt.equals(newSavedAt)) {
            log.debug("SavedForLater item timestamp changed from {} to {}", oldSavedAt, newSavedAt);
            // Could publish a specific update event if needed
        }

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