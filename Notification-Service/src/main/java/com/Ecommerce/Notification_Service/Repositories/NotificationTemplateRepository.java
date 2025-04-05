package com.Ecommerce.Notification_Service.Repositories;


import com.Ecommerce.Notification_Service.Models.NotificationTemplate;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {
    Optional<NotificationTemplate> findByType(NotificationType type);
}
