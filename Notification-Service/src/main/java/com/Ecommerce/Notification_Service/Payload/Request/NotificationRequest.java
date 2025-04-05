package com.Ecommerce.Notification_Service.Payload.Request;


import com.Ecommerce.Notification_Service.Models.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationRequest {
    private UUID userId;
    private NotificationType type;
    private String content;
    private LocalDateTime expiresAt;

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}