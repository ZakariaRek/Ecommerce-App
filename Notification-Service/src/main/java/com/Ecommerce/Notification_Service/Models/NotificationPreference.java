package com.Ecommerce.Notification_Service.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;
@Data
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
}
