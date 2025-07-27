package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSenderService {

    private final EmailService emailService;
    private final SSENotificationService sseNotificationService;

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
        }
    }

    /**
     * Send email notification using EmailService
     */
    private void sendEmail(Notification notification, String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("Cannot send email notification - user email is null or empty for notification: {}", notification.getId());
            return;
        }

        try {
            emailService.sendNotificationEmail(notification, userEmail);
            log.info("Email notification sent successfully to: {} for notification: {}", userEmail, notification.getId());
        } catch (Exception e) {
            log.error("Failed to send email notification to: {} for notification: {}", userEmail, notification.getId(), e);
            throw new RuntimeException("Failed to send email notification", e);
        }
    }

    /**
     * Send SMS notification (placeholder implementation)
     */
    private void sendSms(Notification notification) {
        // TODO: Implement SMS sending logic using Twilio or other SMS service
        log.info("SMS notification would be sent: {}", notification.getContent());

        // Example implementation would be:
        // smsService.sendSms(userPhoneNumber, notification.getContent());
    }

    /**
     * Send push notification (placeholder implementation)
     */
    private void sendPushNotification(Notification notification) {
        // TODO: Implement push notification logic using Firebase or other push service
        log.info("Push notification would be sent: {}", notification.getContent());

        // Example implementation would be:
        // firebaseService.sendPushNotification(deviceToken, notification.getContent());
    }

    /**
     * Send in-app notification via SSE
     */
    private void sendInAppNotification(Notification notification) {
        try {
            sseNotificationService.sendNotificationToUser(notification.getUserId(), notification);
            log.info("In-app notification sent via SSE to user: {} for notification: {}",
                    notification.getUserId(), notification.getId());
        } catch (Exception e) {
            log.error("Failed to send in-app notification to user: {} for notification: {}",
                    notification.getUserId(), notification.getId(), e);
        }
    }

    /**
     * Send notification through multiple channels
     */
    public void sendMultiChannel(Notification notification, String userEmail, NotificationChannel... channels) {
        for (NotificationChannel channel : channels) {
            try {
                send(notification, channel, userEmail);
            } catch (Exception e) {
                log.error("Failed to send notification through channel: {} for notification: {}",
                        channel, notification.getId(), e);
                // Continue with other channels even if one fails
            }
        }
    }

    /**
     * Send bulk notifications through specified channel
     */
    public void sendBulk(java.util.List<Notification> notifications, NotificationChannel channel,
                         java.util.Map<String, String> userEmails) {

        for (Notification notification : notifications) {
            try {
                String userEmail = userEmails.get(notification.getUserId().toString());
                send(notification, channel, userEmail);
            } catch (Exception e) {
                log.error("Failed to send bulk notification for user: {} through channel: {}",
                        notification.getUserId(), channel, e);
                // Continue with other notifications even if one fails
            }
        }
    }

    /**
     * Check if channel is available and properly configured
     */
    public boolean isChannelAvailable(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> true; // EmailService is configured
            case IN_APP -> true; // SSE service is configured
            case SMS -> false; // TODO: Implement SMS service
            case PUSH -> false; // TODO: Implement push notification service
        };
    }

    /**
     * Get channel status
     */
    public String getChannelStatus(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> "ACTIVE - Using Spring Mail";
            case IN_APP -> "ACTIVE - Using SSE";
            case SMS -> "INACTIVE - Service not implemented";
            case PUSH -> "INACTIVE - Service not implemented";
        };
    }
}