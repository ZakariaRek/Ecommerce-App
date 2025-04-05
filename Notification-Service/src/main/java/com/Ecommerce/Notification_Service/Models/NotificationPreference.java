package com.Ecommerce.Notification_Service.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "notification_preferences")
public class NotificationPreference {
    @Id
    private String id;
    private UUID userId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private boolean isEnabled;

    // Constructors
    public NotificationPreference() {
    }

    public NotificationPreference(UUID userId, NotificationType notificationType, NotificationChannel channel, boolean isEnabled) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.isEnabled = isEnabled;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
