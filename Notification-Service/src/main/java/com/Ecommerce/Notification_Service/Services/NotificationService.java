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
import java.util.*;

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

    @Autowired
    private SSENotificationService sseNotificationService;

    public List<Notification> getAllNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserIdAndIsRead(userId, false);
    }

    public Notification getNotificationById(String id) {
        return notificationRepository.findById(id).orElse(null);
    }

    /**
     * Create notification and send via SSE if user is connected
     */
    public Notification createNotification(UUID userId, NotificationType type, String content, LocalDateTime expiresAt) {
        Notification notification = new Notification(userId, type, content, expiresAt);
        Notification savedNotification = notificationRepository.save(notification);

        // Send real-time notification via SSE
        sseNotificationService.sendNotificationToUser(userId, savedNotification);

        // The Kafka event will be automatically published by the MongoDB listener
        return savedNotification;
    }

    /**
     * Create notification for product-related events
     */
    public Notification createProductNotification(UUID userId, NotificationType type, String content,
                                                  String productId, String productName, LocalDateTime expiresAt) {
        // Enhanced content with product information
        String enhancedContent = String.format("[Product: %s] %s", productName, content);

        Notification notification = new Notification(userId, type, enhancedContent, expiresAt);
        Notification savedNotification = notificationRepository.save(notification);

        // Send real-time notification via SSE with product context
        sseNotificationService.sendNotificationToUser(userId, savedNotification);

        return savedNotification;
    }

    /**
     * Create inventory-related notification
     */
    public Notification createInventoryNotification(UUID userId, NotificationType type, String productName,
                                                    Integer currentStock, Integer threshold, String warehouseLocation) {
        String content = switch (type) {
            case INVENTORY_LOW_STOCK -> String.format("Low stock alert for %s. Current: %d, Threshold: %d (Warehouse: %s)",
                    productName, currentStock, threshold, warehouseLocation);
            case INVENTORY_OUT_OF_STOCK -> String.format("%s is out of stock in %s", productName, warehouseLocation);
            case INVENTORY_RESTOCKED -> String.format("%s has been restocked. New quantity: %d (Warehouse: %s)",
                    productName, currentStock, warehouseLocation);
            default -> String.format("Inventory update for %s", productName);
        };

        Notification notification = new Notification(userId, type, content, LocalDateTime.now().plusDays(3));
        Notification savedNotification = notificationRepository.save(notification);

        // Send real-time notification via SSE
        sseNotificationService.sendNotificationToUser(userId, savedNotification);

        return savedNotification;
    }

    /**
     * Create discount-related notification
     */
    public Notification createDiscountNotification(UUID userId, NotificationType type, String productName,
                                                   String discountValue, String discountType) {
        String content = switch (type) {
            case DISCOUNT_ACTIVATED -> String.format("New %s discount of %s available for %s",
                    discountType, discountValue, productName);
            case DISCOUNT_DEACTIVATED -> String.format("Discount for %s has ended", productName);
            case DISCOUNT_EXPIRED -> String.format("Your %s discount for %s has expired", discountType, productName);
            default -> String.format("Discount update for %s", productName);
        };

        Notification notification = new Notification(userId, type, content, LocalDateTime.now().plusDays(1));
        Notification savedNotification = notificationRepository.save(notification);

        // Send real-time notification via SSE
        sseNotificationService.sendNotificationToUser(userId, savedNotification);

        return savedNotification;
    }

    /**
     * Send notification through all configured channels
     */
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

    /**
     * Mark notification as read and update via SSE
     */
    public Notification markAsRead(String id) {
        Optional<Notification> optionalNotification = notificationRepository.findById(id);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();

            // Store the notification state before saving for event tracking
            notificationMongoListener.storeStateBeforeSave(notification);

            notification.setRead(true);
            Notification updatedNotification = notificationRepository.save(notification);

            // Send update via SSE
            sseNotificationService.sendNotificationToUser(notification.getUserId(), updatedNotification);

            return updatedNotification;
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
     * Send bulk notifications to multiple users with SSE support
     */
    public void sendBulkNotifications(List<UUID> userIds, NotificationType type, String content, LocalDateTime expiresAt) {
        for (UUID userId : userIds) {
            // Create notification with SSE support
            createNotification(userId, type, content, expiresAt);

            // Also send through other channels if enabled
            for (NotificationChannel channel : NotificationChannel.values()) {
                if (preferenceService.isChannelEnabled(userId, type, channel)) {
                    Notification notification = new Notification(userId, type, content, expiresAt);
                    senderService.send(notification, channel);
                }
            }
        }

        // Publish a bulk notification event
        kafkaService.publishBulkNotificationSent(type, content, userIds.size());
    }

    /**
     * Broadcast system notification to all connected users via SSE
     */
    public void broadcastSystemNotification(String title, String message, NotificationType type) {
        // Send via SSE to all connected users
        sseNotificationService.sendSystemAlert(title, message, type.name());

        // Also create persistent notifications for important system messages
        if (type == NotificationType.SYSTEM_ALERT) {
            // This would require getting all active user IDs - implement based on your user management
            // For now, we'll just broadcast via SSE
        }
    }

    /**
     * Create notification from Kafka events with automatic SSE delivery
     */
    public Notification createNotificationFromKafkaEvent(UUID userId, NotificationType type, String content,
                                                         LocalDateTime expiresAt, Map<String, Object> metadata) {
        Notification notification = new Notification(userId, type, content, expiresAt);
        Notification savedNotification = notificationRepository.save(notification);

        // Automatically send via SSE
        sseNotificationService.sendNotificationToUser(userId, savedNotification);

        return savedNotification;
    }

    /**
     * Get real-time notification statistics
     */
    public Map<String, Object> getNotificationStats(UUID userId) {
        List<Notification> userNotifications = getAllNotificationsByUserId(userId);
        List<Notification> unreadNotifications = getUnreadNotificationsByUserId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotifications", userNotifications.size());
        stats.put("unreadCount", unreadNotifications.size());
        stats.put("readCount", userNotifications.size() - unreadNotifications.size());
        stats.put("sseConnections", sseNotificationService.getUserConnectionCount(userId));
        stats.put("lastNotification", userNotifications.isEmpty() ? null : userNotifications.get(0).getCreatedAt());

        return stats;
    }
}