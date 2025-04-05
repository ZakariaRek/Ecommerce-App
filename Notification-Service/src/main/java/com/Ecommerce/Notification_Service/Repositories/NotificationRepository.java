package com.Ecommerce.Notification_Service.Repositories;

import com.Ecommerce.Notification_Service.Models.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserId(UUID userId);
    List<Notification> findByUserIdAndIsRead(UUID userId, boolean isRead);
}
