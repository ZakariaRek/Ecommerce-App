package com.Ecommerce.Notification_Service.Payload.Request;


import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import com.Ecommerce.Notification_Service.Models.NotificationType;

import java.util.UUID;

public class PreferenceRequest {
    private UUID userId;
    private NotificationType notificationType;
    private NotificationChannel channel;
    private boolean enabled;

    // Getters and Setters
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
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
