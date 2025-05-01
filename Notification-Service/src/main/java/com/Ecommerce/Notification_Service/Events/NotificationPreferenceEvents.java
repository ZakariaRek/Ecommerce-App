package com.Ecommerce.Notification_Service.Events;

import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import com.Ecommerce.Notification_Service.Models.NotificationPreference;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationPreferenceEvents {

    /**
     * Base event for all notification preference events
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public abstract static class NotificationPreferenceEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private UUID userId;

        public NotificationPreferenceEvent(String eventType) {
            this.eventId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
        }
    }

    /**
     * Event fired when a notification preference is created
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceCreatedEvent extends NotificationPreferenceEvent {
        private String preferenceId;
        private UUID userId;
        private NotificationType notificationType;
        private NotificationChannel channel;
        private boolean enabled;

        public PreferenceCreatedEvent(NotificationPreference preference) {
            super("PREFERENCE_CREATED");
            this.preferenceId = preference.getId();
            this.userId = preference.getUserId();
            this.notificationType = preference.getNotificationType();
            this.channel = preference.getChannel();
            this.enabled = preference.isEnabled();
        }
    }

    /**
     * Event fired when a notification preference is updated
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceUpdatedEvent extends NotificationPreferenceEvent {
        private String preferenceId;
        private UUID userId;
        private NotificationType notificationType;
        private NotificationChannel channel;
        private boolean oldEnabled;
        private boolean newEnabled;

        public PreferenceUpdatedEvent(NotificationPreference preference, boolean oldEnabled) {
            super("PREFERENCE_UPDATED");
            this.preferenceId = preference.getId();
            this.userId = preference.getUserId();
            this.notificationType = preference.getNotificationType();
            this.channel = preference.getChannel();
            this.oldEnabled = oldEnabled;
            this.newEnabled = preference.isEnabled();
        }
    }

    /**
     * Event fired when a user opts out of all notifications
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOptedOutEvent extends NotificationPreferenceEvent {
        private UUID userId;
        private LocalDateTime optedOutAt;
        private String reason;

        public UserOptedOutEvent(UUID userId, String reason) {
            super("USER_OPTED_OUT");
            this.userId = userId;
            this.optedOutAt = LocalDateTime.now();
            this.reason = reason;
        }
    }
}