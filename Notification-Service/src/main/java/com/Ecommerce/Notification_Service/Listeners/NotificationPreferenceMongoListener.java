package com.Ecommerce.Notification_Service.Listeners;

import com.Ecommerce.Notification_Service.Models.NotificationPreference;
import com.Ecommerce.Notification_Service.Services.Kafka.NotificationPreferenceKafkaService;
import com.Ecommerce.Notification_Service.Services.NotificationPreferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for NotificationPreference documents to automatically publish events to Kafka
 * when preferences are created or updated
 */
@Slf4j
@Component
public class NotificationPreferenceMongoListener extends AbstractMongoEventListener<NotificationPreference> {

    private final NotificationPreferenceKafkaService kafkaService;

    // Using @Lazy to break circular dependency
    @Lazy
    @Autowired
    private NotificationPreferenceService preferenceService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    // Constructor with required dependency
    @Autowired
    public NotificationPreferenceMongoListener(NotificationPreferenceKafkaService kafkaService) {
        this.kafkaService = kafkaService;
    }

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<NotificationPreference> event) {
        NotificationPreference preference = event.getSource();
        String key = getEntityKey(preference);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update
                handlePreferenceUpdate(preference, oldState);
            } else {
                // This is a new preference
                handlePreferenceCreation(preference);
            }
        } catch (Exception e) {
            log.error("Error in notification preference MongoDB listener after save", e);
        }
    }

    /**
     * Handle the creation of a new notification preference
     */
    private void handlePreferenceCreation(NotificationPreference preference) {
        kafkaService.publishPreferenceCreated(preference);
        log.debug("MongoDB listener triggered for preference creation: {}", preference.getId());
    }

    /**
     * Handle updates to an existing notification preference
     */
    private void handlePreferenceUpdate(NotificationPreference preference, EntityState oldState) {
        // Check what changed
        boolean wasEnabled = oldState.isEnabled;
        boolean isEnabledNow = preference.isEnabled();

        if (wasEnabled != isEnabledNow) {
            // Preference enabled status changed
            kafkaService.publishPreferenceUpdated(preference, wasEnabled);
            log.debug("MongoDB listener triggered for preference enabled status change: {}", preference.getId());
        }

        log.debug("MongoDB listener triggered for preference update: {}", preference.getId());
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(NotificationPreference preference) {
        String key = getEntityKey(preference);
        entityStateMap.put(key, new EntityState(preference.isEnabled()));
        log.debug("Stored state before preference save: {}", preference.getId());
    }

    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(NotificationPreference preference) {
        return "NotificationPreference:" + preference.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final boolean isEnabled;

        public EntityState(boolean isEnabled) {
            this.isEnabled = isEnabled;
        }
    }
}