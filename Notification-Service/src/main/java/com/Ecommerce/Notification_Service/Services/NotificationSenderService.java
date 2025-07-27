package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSenderService {

    private final EmailService emailService;
    // Optional: Inject SSE service if it exists
    // private final SSENotificationService sseNotificationService;

    /**
     * Send notification through the specified channel
     */
    public void send(Notification notification, NotificationChannel channel) {
        send(notification, channel, null);
    }

    /**
     * Send notification through the specified channel with user email
     */
    public void send(Notification notification, NotificationChannel channel, String userEmail) {
        if (notification == null) {
            log.warn("Cannot send notification - notification is null");
            return;
        }

        try {
            switch (channel) {
                case EMAIL:
                    sendEmail(notification, userEmail);
                    break;
                case SMS:
                    sendSms(notification);
                    break;
                case PUSH:
                    sendPushNotification(notification);
                    break;
                case IN_APP:
                    sendInAppNotification(notification);
                    break;
                default:
                    log.warn("Unknown notification channel: {}", channel);
            }
        } catch (Exception e) {
            log.error("Failed to send notification through channel: {} for notification: {}",
                    channel, notification.getId(), e);
            throw new RuntimeException("Failed to send notification via " + channel, e);
        }
    }

    /**
     * Send email notification using EmailService
     */
    private void sendEmail(Notification notification, String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("Cannot send email notification - user email is null or empty for notification: {}",
                    notification.getId());
            return;
        }

        try {
            // Generate subject based on notification type
            String subject = generateEmailSubject(notification);

            // Use the existing sendEmailToUser method
            emailService.sendEmailToUser(
                    userEmail,
                    subject,
                    notification.getContent(),
                    notification.getType()
            );

            log.info("Email notification sent successfully to: {} for notification: {}",
                    userEmail, notification.getId());

        } catch (Exception e) {
            log.error("Failed to send email notification to: {} for notification: {}",
                    userEmail, notification.getId(), e);
            throw new RuntimeException("Failed to send email notification", e);
        }
    }

    /**
     * Send SMS notification (placeholder implementation)
     */
    private void sendSms(Notification notification) {
        // TODO: Implement SMS sending logic using Twilio or other SMS service
        log.info("SMS notification would be sent for notification {}: {}",
                notification.getId(), notification.getContent());

        // Example implementation would be:
        // smsService.sendSms(userPhoneNumber, notification.getContent());

        // For now, just log that SMS would be sent
        log.debug("SMS Service not implemented. Content: {}", notification.getContent());
    }

    /**
     * Send push notification (placeholder implementation)
     */
    private void sendPushNotification(Notification notification) {
        // TODO: Implement push notification logic using Firebase or other push service
        log.info("Push notification would be sent for notification {}: {}",
                notification.getId(), notification.getContent());

        // Example implementation would be:
        // firebaseService.sendPushNotification(deviceToken, notification.getContent());

        // For now, just log that push notification would be sent
        log.debug("Push Notification Service not implemented. Content: {}", notification.getContent());
    }

    /**
     * Send in-app notification via SSE (placeholder implementation)
     */
    private void sendInAppNotification(Notification notification) {
        try {
            // TODO: Implement SSE notification service
            // if (sseNotificationService != null) {
            //     sseNotificationService.sendNotificationToUser(notification.getUserId(), notification);
            //     log.info("In-app notification sent via SSE to user: {} for notification: {}",
            //             notification.getUserId(), notification.getId());
            // } else {
            log.info("In-app notification would be sent via SSE to user: {} for notification: {}",
                    notification.getUserId(), notification.getId());
            log.debug("SSE Service not implemented. Content: {}", notification.getContent());
            // }
        } catch (Exception e) {
            log.error("Failed to send in-app notification to user: {} for notification: {}",
                    notification.getUserId(), notification.getId(), e);
            throw new RuntimeException("Failed to send in-app notification", e);
        }
    }

    /**
     * Send notification through multiple channels
     */
    public void sendMultiChannel(Notification notification, String userEmail, NotificationChannel... channels) {
        if (notification == null) {
            log.warn("Cannot send multi-channel notification - notification is null");
            return;
        }

        if (channels == null || channels.length == 0) {
            log.warn("No channels specified for multi-channel notification: {}", notification.getId());
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (NotificationChannel channel : channels) {
            try {
                send(notification, channel, userEmail);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send notification through channel: {} for notification: {}",
                        channel, notification.getId(), e);
                // Continue with other channels even if one fails
            }
        }

        log.info("Multi-channel notification completed for notification: {}. Success: {}, Failures: {}",
                notification.getId(), successCount, failureCount);
    }

    /**
     * Send bulk notifications through specified channel
     */
    public void sendBulk(List<Notification> notifications, NotificationChannel channel,
                         Map<String, String> userEmails) {

        if (notifications == null || notifications.isEmpty()) {
            log.warn("Cannot send bulk notifications - notifications list is null or empty");
            return;
        }

        if (userEmails == null) {
            log.warn("Cannot send bulk notifications - userEmails map is null");
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (Notification notification : notifications) {
            try {
                if (notification.getUserId() == null) {
                    log.warn("Skipping notification with null userId: {}", notification.getId());
                    failureCount++;
                    continue;
                }

                String userEmail = userEmails.get(notification.getUserId().toString());
                send(notification, channel, userEmail);
                successCount++;

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send bulk notification for user: {} through channel: {}",
                        notification.getUserId(), channel, e);
                // Continue with other notifications even if one fails
            }
        }

        log.info("Bulk notification completed. Channel: {}, Total: {}, Success: {}, Failures: {}",
                channel, notifications.size(), successCount, failureCount);
    }

    /**
     * Send bulk notifications with user email lookup
     */
    public void sendBulkWithEmailLookup(List<Notification> notifications, NotificationChannel channel) {
        if (notifications == null || notifications.isEmpty()) {
            log.warn("Cannot send bulk notifications - notifications list is null or empty");
            return;
        }

        // TODO: Implement user email lookup service
        // For now, log that this would require user service integration
        log.info("Bulk notification with email lookup would be sent. Channel: {}, Count: {}",
                channel, notifications.size());
        log.debug("Email lookup service not implemented. Use sendBulk() with userEmails map instead.");
    }

    /**
     * Check if channel is available and properly configured
     */
    public boolean isChannelAvailable(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> true; // EmailService is configured
            case IN_APP -> false; // TODO: Check if SSE service is configured
            case SMS -> false; // TODO: Implement SMS service
            case PUSH -> false; // TODO: Implement push notification service
        };
    }

    /**
     * Get channel status with details
     */
    public String getChannelStatus(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> "ACTIVE - Using Spring Mail with configured templates";
            case IN_APP -> "INACTIVE - SSE service not implemented";
            case SMS -> "INACTIVE - SMS service not implemented (requires Twilio/similar)";
            case PUSH -> "INACTIVE - Push notification service not implemented (requires Firebase/similar)";
        };
    }

    /**
     * Get detailed channel information
     */
    public Map<String, Object> getChannelInfo(NotificationChannel channel) {
        boolean available = isChannelAvailable(channel);
        String status = getChannelStatus(channel);

        return Map.of(
                "channel", channel.name(),
                "available", available,
                "status", status,
                "description", getChannelDescription(channel)
        );
    }

    /**
     * Get all channels status
     */
    public Map<NotificationChannel, Map<String, Object>> getAllChannelsStatus() {
        Map<NotificationChannel, Map<String, Object>> statusMap = new java.util.HashMap<>();

        for (NotificationChannel channel : NotificationChannel.values()) {
            statusMap.put(channel, getChannelInfo(channel));
        }

        return statusMap;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate email subject based on notification type
     */
    private String generateEmailSubject(Notification notification) {
        if (notification.getType() == null) {
            return "Notification from " + getServiceName();
        }

        return switch (notification.getType()) {
            case PAYMENT_CONFIRMATION -> "💳 Payment Confirmation";
            case SHIPPING_UPDATE -> "🚚 Shipping Update";
            case PROMOTION -> "🎉 Special Offer";
            case ACCOUNT_ACTIVITY -> "👤 Account Activity";
            default -> "📧 " + notification.getType().name().replace("_", " ");
        };
    }

    /**
     * Get channel description
     */
    private String getChannelDescription(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> "Email notifications using SMTP server";
            case SMS -> "SMS notifications via third-party service";
            case PUSH -> "Push notifications for mobile devices";
            case IN_APP -> "Real-time in-app notifications via Server-Sent Events";
        };
    }

    /**
     * Get service name (configurable)
     */
    private String getServiceName() {
        return "E-Commerce Platform";
    }

    /**
     * Test notification sending for a specific channel
     */
    public boolean testChannel(NotificationChannel channel, String testEmail) {
        try {
            // Create a test notification
            Notification testNotification = createTestNotification();

            send(testNotification, channel, testEmail);
            return true;

        } catch (Exception e) {
            log.error("Channel test failed for {}: {}", channel, e.getMessage());
            return false;
        }
    }

    /**
     * Create a test notification
     */
    private Notification createTestNotification() {
        // This would normally create a proper Notification entity
        // For now, return a mock or create based on your Notification model

        // TODO: Replace with actual Notification entity creation
        // return Notification.builder()
        //         .id(UUID.randomUUID())
        //         .userId(UUID.randomUUID())
        //         .type(NotificationType.ACCOUNT_ACTIVITY)
        //         .content("This is a test notification from the notification service.")
        //         .build();

        log.debug("Test notification would be created here");
        return null; // Placeholder - implement based on your Notification model
    }
}