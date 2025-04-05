package com.Ecommerce.Notification_Service.Repositories;


import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import com.Ecommerce.Notification_Service.Models.NotificationPreference;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreference, String> {
    List<NotificationPreference> findByUserId(UUID userId);
    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndChannel(UUID userId, NotificationType type, NotificationChannel channel);
}