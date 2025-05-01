package com.Ecommerce.Notification_Service.Listeners;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Services.Kafka.NotificationKafkaService;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for Notification documents to automatically publish events to Kafka
 * when notifications are created, read, or deleted
 */
@Slf4j
@Component
public class NotificationMongoListener extends AbstractMongoEventListener<Notification> {

    private final NotificationKafkaService kafkaService;

    // Using @Lazy to break circular dependency
    @Lazy
    @Autowired
    private NotificationService notificationService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    // Store notifications before deletion to use in after-delete events
    private static final Map<String, Notification> preDeleteNotificationMap = new ConcurrentHashMap<>();

    // Constructor with required dependency
    @Autowired
    public NotificationMongoListener(NotificationKafkaService kafkaService) {
        this.kafkaService = kafkaService;
    }

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<Notification> event) {
        Notification notification = event.getSource();
        String key = getEntityKey(notification);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update
                handleNotificationUpdate(notification, oldState);
            } else {
                // This is a new notification
                handleNotificationCreation(notification);
            }
        } catch (Exception e) {
            log.error("Error in notification MongoDB listener after save", e);
        }
    }

    /**
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<Notification> event) {
        try {
            // Extract the ID from the deleted document
            Object id = event.getSource().get("_id");
            if (id != null) {
                String documentId = id.toString();
                log.debug("Notification with id: {} was deleted", documentId);

                // Look for the stored Notification from the beforeDelete event
                String key = "Notification:" + documentId;
                Notification deletedNotification = preDeleteNotificationMap.remove(key);

                if (deletedNotification != null) {
                    // Now we have the complete notification that was deleted, publish the event
                    kafkaService.publishNotificationDeleted(deletedNotification, "user_deleted");
                    log.debug("Published Kafka event for deleted notification: {}", documentId);
                } else {
                    log.warn("Could not find pre-delete state for notification with id: {}", documentId);
                }
            }
        } catch (Exception e) {
            log.error("Error in notification MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new notification
     */
    private void handleNotificationCreation(Notification notification) {
        kafkaService.publishNotificationCreated(notification);
        log.debug("MongoDB listener triggered for notification creation: {}", notification.getId());
    }

    /**
     * Handle updates to an existing notification
     */
    private void handleNotificationUpdate(Notification notification, EntityState oldState) {
        // Check what changed
        boolean wasRead = oldState.isRead;
        boolean isReadNow = notification.isRead();

        if (!wasRead && isReadNow) {
            // Notification was marked as read
            kafkaService.publishNotificationRead(notification);
            log.debug("MongoDB listener triggered for notification read: {}", notification.getId());
        }

        log.debug("MongoDB listener triggered for notification update: {}", notification.getId());
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(Notification notification) {
        String key = getEntityKey(notification);
        entityStateMap.put(key, new EntityState(notification.isRead()));
        log.debug("Stored state before notification save: {}", notification.getId());
    }

    /**
     * Store notification before delete for use in afterDelete
     * This should be called by the service layer before deleting
     */
    public void storeBeforeDelete(Notification notification) {
        String key = "Notification:" + notification.getId();
        preDeleteNotificationMap.put(key, notification);
        log.debug("Stored notification before delete: {}", notification.getId());
    }

    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(Notification notification) {
        return "Notification:" + notification.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final boolean isRead;

        public EntityState(boolean isRead) {
            this.isRead = isRead;
        }
    }
}