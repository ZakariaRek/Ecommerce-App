package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Listeners.NotificationMongoListener;
import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Repositories.NotificationRepository;
import com.Ecommerce.Notification_Service.Services.Kafka.NotificationKafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceService preferenceService;

    @Autowired
    private NotificationTemplateService templateService;

    @Autowired
    private NotificationSenderService senderService;

    @Autowired
    private NotificationKafkaService kafkaService;

    @Autowired
    private NotificationMongoListener notificationMongoListener;

    public List<Notification> getAllNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserIdAndIsRead(userId, false);
    }

    public Notification getNotificationById(String id) {
        return notificationRepository.findById(id).orElse(null);
    }

    public Notification createNotification(UUID userId, NotificationType type, String content, LocalDateTime expiresAt) {
        Notification notification = new Notification(userId, type, content, expiresAt);
        Notification savedNotification = notificationRepository.save(notification);

        // The Kafka event will be automatically published by the MongoDB listener

        return savedNotification;
    }

    public Notification sendNotification(UUID userId, NotificationType type, String content, LocalDateTime expiresAt) {
        Notification notification = createNotification(userId, type, content, expiresAt);

        // Check user preferences for each channel
        for (NotificationChannel channel : NotificationChannel.values()) {
            if (preferenceService.isChannelEnabled(userId, type, channel)) {
                senderService.send(notification, channel);
            }
        }

        return notification;
    }

    public Notification markAsRead(String id) {
        Optional<Notification> optionalNotification = notificationRepository.findById(id);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();

            // Store the notification state before saving for event tracking
            notificationMongoListener.storeStateBeforeSave(notification);

            notification.setRead(true);
            return notificationRepository.save(notification);
        }
        return null;
    }

    public void deleteNotification(String id) {
        Optional<Notification> optionalNotification = notificationRepository.findById(id);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();

            // Store the notification before deleting for event tracking
            notificationMongoListener.storeBeforeDelete(notification);

            notificationRepository.deleteById(id);
        }
    }

    /**
     * Send bulk notifications to multiple users
     */
    public void sendBulkNotifications(List<UUID> userIds, NotificationType type, String content, LocalDateTime expiresAt) {
        for (UUID userId : userIds) {
            sendNotification(userId, type, content, expiresAt);
        }

        // Publish a bulk notification event
        kafkaService.publishBulkNotificationSent(type, content, userIds.size());
    }
}