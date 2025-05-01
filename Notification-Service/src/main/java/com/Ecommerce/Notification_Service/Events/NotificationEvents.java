package com.Ecommerce.Notification_Service.Events;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationEvents {

    /**
     * Base event for all notification events
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class NotificationEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private UUID userId;
        private UUID sessionId;

        public NotificationEvent(String eventType) {
            this.eventId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
        }
    }

    /**
     * Event fired when a notification is created
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationCreatedEvent extends NotificationEvent {
        private String notificationId;
        private UUID userId;
        private NotificationType type;
        private String content;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public NotificationCreatedEvent(Notification notification) {
            super("NOTIFICATION_CREATED");
            this.notificationId = notification.getId();
            this.userId = notification.getUserId();
            this.type = notification.getType();
            this.content = notification.getContent();
            this.createdAt = notification.getCreatedAt();
            this.expiresAt = notification.getExpiresAt();
        }
    }

    /**
     * Event fired when a notification is read
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationReadEvent extends NotificationEvent {
        private String notificationId;
        private UUID userId;
        private NotificationType type;
        private LocalDateTime readAt;

        public NotificationReadEvent(Notification notification) {
            super("NOTIFICATION_READ");
            this.notificationId = notification.getId();
            this.userId = notification.getUserId();
            this.type = notification.getType();
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * Event fired when a notification is deleted
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationDeletedEvent extends NotificationEvent {
        private String notificationId;
        private UUID userId;
        private NotificationType type;
        private LocalDateTime deletedAt;
        private String deletionReason;

        public NotificationDeletedEvent(Notification notification, String deletionReason) {
            super("NOTIFICATION_DELETED");
            this.notificationId = notification.getId();
            this.userId = notification.getUserId();
            this.type = notification.getType();
            this.deletedAt = LocalDateTime.now();
            this.deletionReason = deletionReason;
        }
    }

    /**
     * Event fired when bulk notifications are sent
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkNotificationSentEvent extends NotificationEvent {
        private int recipientCount;
        private NotificationType type;
        private String content;
        private LocalDateTime sentAt;

        public BulkNotificationSentEvent(NotificationType type, String content, int recipientCount) {
            super("BULK_NOTIFICATION_SENT");
            this.type = type;
            this.content = content;
            this.recipientCount = recipientCount;
            this.sentAt = LocalDateTime.now();
        }
    }
}