package com.Ecommerce.Notification_Service.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;
@Data

@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private UUID userId;
    private NotificationType type;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Constructors
    public Notification() {
    }

    public Notification(UUID userId, NotificationType type, String content, LocalDateTime expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

}