package com.Ecommerce.Notification_Service.Listeners.AsynCom;

import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Services.EmailService;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import com.Ecommerce.Notification_Service.Services.UserEmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailKafkaConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserEmailService userEmailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-confirmed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentConfirmed(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());
            String orderId = event.path("orderId").asText();
            String amount = event.path("amount").asText();
            String paymentMethod = event.path("paymentMethod").asText("Credit Card");

            String content = String.format("Payment of %s has been confirmed for order #%s. Thank you for your purchase!", amount, orderId);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_CONFIRMATION,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send automatic payment confirmation email
            sendEmailNotificationAsync(userId, (email) ->
                            emailService.sendPaymentConfirmationEmail(null, orderId, amount, paymentMethod),
                    "payment confirmation");

            log.info("Processed payment confirmation notification. Order: {}, User: {}, Amount: {}", orderId, userId, amount);

        } catch (Exception e) {
            log.error("Error processing payment confirmation event", e);
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailed(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());
            String orderId = event.path("orderId").asText();
            String reason = event.path("reason").asText("Payment processing error");

            String content = String.format("Payment failed for order #%s. Reason: %s. Please try again or contact support.", orderId, reason);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_CONFIRMATION,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send automatic payment failure email
            sendEmailNotificationAsync(userId, (email) ->
                            emailService.sendEmailToUser(null, "❌ Payment Failed - Order #" + orderId, content, NotificationType.PAYMENT_CONFIRMATION),
                    "payment failure");

            log.info("Processed payment failed notification. Order: {}, User: {}", orderId, userId);

        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }

    // ================ SHIPPING SERVICE EVENTS ================

    @KafkaListener(topics = "shipping-update", groupId = "${spring.kafka.consumer.group-id}")
    public void handleShippingUpdate(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());
            String orderId = event.path("orderId").asText();
            String status = event.path("status").asText();
            String trackingNumber = event.path("trackingNumber").asText("");

            String content;
            if (trackingNumber != null && !trackingNumber.isEmpty()) {
                content = String.format("Shipping update for order #%s: %s. Tracking number: %s", orderId, status, trackingNumber);
            } else {
                content = String.format("Shipping update for order #%s: %s", orderId, status);
            }

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.SHIPPING_UPDATE,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send automatic shipping update email
            sendEmailNotificationAsync(userId, (email) ->
                            emailService.sendShippingUpdateEmail(null, orderId, status, trackingNumber),
                    "shipping update");

            log.info("Processed shipping update notification. Order: {}, User: {}, Status: {}", orderId, userId, status);

        } catch (Exception e) {
            log.error("Error processing shipping update event", e);
        }
    }

    @KafkaListener(topics = "shipping-delivered", groupId = "${spring.kafka.consumer.group-id}")
    public void handleShippingDelivered(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());
            String orderId = event.path("orderId").asText();
            String deliveryDate = event.path("deliveryDate").asText("");

            String content = String.format("Your order #%s has been delivered successfully! We hope you enjoy your purchase.", orderId);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.SHIPPING_UPDATE,
                    content,
                    LocalDateTime.now().plusDays(30)
            );

            // Send automatic delivery confirmation email
            sendEmailNotificationAsync(userId, (email) ->
                            emailService.sendShippingUpdateEmail(null, orderId, "DELIVERED", ""),
                    "delivery confirmation");

            log.info("Processed delivery notification. Order: {}, User: {}", orderId, userId);

        } catch (Exception e) {
            log.error("Error processing delivery event", e);
        }
    }

    // ================ LOYALTY SERVICE EVENTS ================

    @KafkaListener(topics = "loyalty-points-earned", groupId = "${spring.kafka.consumer.group-id}")
    public void handleLoyaltyPointsEarned(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());
            int pointsEarned = event.path("pointsEarned").asInt();
            int totalPoints = event.path("totalPoints").asInt();
            String reason = event.path("reason").asText("Purchase reward");
            String orderId = event.path("orderId").asText("");

            String content = String.format("You've earned %d loyalty points! %s. Total points: %d", pointsEarned, reason, totalPoints);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.ACCOUNT_ACTIVITY,
                    content,
                    LocalDateTime.now().plusDays(30)
            );

            // Send automatic loyalty points email
            sendEmailNotificationAsync(userId, (email) ->
                            emailService.sendLoyaltyPointsEmail(null, pointsEarned, totalPoints, reason),
                    "loyalty points earned");

            log.info("Processed loyalty points earned notification. User: {}, Points: {}, Total: {}", userId, pointsEarned, totalPoints);

        } catch (Exception e) {
            log.error("Error processing loyalty points earned event", e);
        }
    }

    @KafkaListener(topics = "loyalty-tier-upgraded", groupId = "${spring.kafka.consumer.group-id}")
    public void handleLoyaltyTierUpgraded(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());
            String newTier = event.path("newTier").asText();
            String oldTier = event.path("oldTier").asText("");

            String content = String.format("Congratulations! You've been upgraded to %s tier from %s. Enjoy your new benefits!", newTier, oldTier);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.ACCOUNT_ACTIVITY,
                    content,
                    LocalDateTime.now().plusDays(30)
            );

            // Send automatic tier upgrade email
            sendEmailNotificationAsync(userId, (email) ->
                            emailService.sendAccountActivityEmail(null, "Loyalty Tier Upgrade", content),
                    "loyalty tier upgrade");

            log.info("Processed loyalty tier upgrade notification. User: {}, New Tier: {}", userId, newTier);

        } catch (Exception e) {
            log.error("Error processing loyalty tier upgrade event", e);
        }
    }


    @KafkaListener(topics = "promotion-created", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePromotionCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String promotionTitle = event.path("title").asText();
            String promotionMessage = event.path("message").asText();
            String promoCode = event.path("promoCode").asText("");
            String validUntil = event.path("validUntil").asText("");
            JsonNode targetUsersNode = event.path("targetUsers");

            // Get target users (if specified) or send to all connected users
            if (targetUsersNode.isArray() && targetUsersNode.size() > 0) {
                // Send to specific users
                List<UUID> targetUserIds = new ArrayList<>();
                for (JsonNode userNode : targetUsersNode) {
                    UUID userId = UUID.fromString(userNode.path("userId").asText());
                    targetUserIds.add(userId);

                    // Create in-app notification
                    notificationService.createNotification(
                            userId,
                            NotificationType.PROMOTION,
                            promotionMessage,
                            LocalDateTime.now().plusDays(7)
                    );
                }

                // Send automatic promotional emails in bulk
                sendBulkEmailNotificationAsync(targetUserIds, (emailMap) -> {
                    List<String> emails = new ArrayList<>(emailMap.values());
                    return emailService.sendPromotionalEmail(emails, promotionTitle, promotionMessage, promoCode, validUntil);
                }, "promotional campaign");
            } else {
                // Broadcast to all connected users
                notificationService.broadcastSystemNotification(promotionTitle, promotionMessage, NotificationType.PROMOTION);
                log.info("Promotion broadcast created: {} - email broadcast would require all user IDs", promotionTitle);
            }

            log.info("Processed promotion created notification: {}", promotionTitle);

        } catch (Exception e) {
            log.error("Error processing promotion created event", e);
        }
    }


    /**
     * Send email notification asynchronously after fetching user email
     */
    private void sendEmailNotificationAsync(UUID userId, EmailSender emailSender, String purpose) {
        userEmailService.getUserEmailWithFallback(userId)
                .thenAccept(userEmail -> {
                    if (userEmail != null && !userEmail.isEmpty()) {
                        try {
                            emailSender.sendEmail(userEmail);
                            log.debug("Email sent successfully for {}: {}", purpose, userEmail);
                        } catch (Exception e) {
                            log.error("Failed to send email for {}: {}", purpose, userEmail, e);
                        }
                    } else {
                        log.warn("No email available for user {} for {}", userId, purpose);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Failed to fetch email for user {} for {}", userId, purpose, ex);
                    return null;
                });
    }

    /**
     * Send bulk email notification asynchronously after fetching user emails
     */
    private void sendBulkEmailNotificationAsync(List<UUID> userIds, BulkEmailSender emailSender, String purpose) {
        userEmailService.getBulkUserEmailsWithFallback(userIds, purpose.toUpperCase())
                .thenAccept(emailMap -> {
                    if (!emailMap.isEmpty()) {
                        try {
                            emailSender.sendBulkEmail(emailMap);
                            log.debug("Bulk email sent successfully for {}: {} recipients", purpose, emailMap.size());
                        } catch (Exception e) {
                            log.error("Failed to send bulk email for {}: {} recipients", purpose, emailMap.size(), e);
                        }
                    } else {
                        log.warn("No emails available for {} for {}", userIds.size(), purpose);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Failed to fetch emails for {} users for {}", userIds.size(), purpose, ex);
                    return null;
                });
    }
    @FunctionalInterface
    private interface EmailSender {
        void sendEmail(String userEmail) throws Exception;
    }

    @FunctionalInterface
    private interface BulkEmailSender {
        CompletableFuture<Void> sendBulkEmail(Map<UUID, String> emailMap) throws Exception;
    }
}
    /**
     * Send notification to all connected users (in-app only)



    @KafkaListener(topics = "order-status-changed", groupId = "${spring.kafka.consumer.group-id}")
    public void listenOrderStatusChanged(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID userId = UUID.fromString(eventNode.path("userId").asText());
            String orderId = eventNode.path("orderId").asText();
            String newStatus = eventNode.path("newStatus").asText();

            String content = String.format("Your order #%s has been updated to status: %s", orderId, newStatus);

            notificationService.createNotification(
                    userId,
                    NotificationType.ORDER_STATUS,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            log.info("Created notification for order status change. Order ID: {}, User ID: {}", orderId, userId);
        } catch (Exception e) {
            log.error("Error processing order status change event", e);
        }
    }

    @KafkaListener(topics = "payment-confirmed", groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentConfirmed(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID userId = UUID.fromString(eventNode.path("userId").asText());
            String orderId = eventNode.path("orderId").asText();
            String amount = eventNode.path("amount").asText();

            String content = String.format("Payment of %s has been confirmed for order #%s. Thank you for your purchase!", amount, orderId);

            notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_CONFIRMATION,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            log.info("Created notification for payment confirmation. Order ID: {}, User ID: {}", orderId, userId);
        } catch (Exception e) {
            log.error("Error processing payment confirmation event", e);
        }
    }

    @KafkaListener(topics = "shipping-update", groupId = "${spring.kafka.consumer.group-id}")
    public void listenShippingUpdate(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID userId = UUID.fromString(eventNode.path("userId").asText());
            String orderId = eventNode.path("orderId").asText();
            String status = eventNode.path("status").asText();
            String trackingNumber = eventNode.path("trackingNumber").asText();

            String content;
            if (trackingNumber != null && !trackingNumber.isEmpty()) {
                content = String.format("Shipping update for order #%s: %s. Tracking number: %s",
                        orderId, status, trackingNumber);
            } else {
                content = String.format("Shipping update for order #%s: %s", orderId, status);
            }

            notificationService.createNotification(
                    userId,
                    NotificationType.SHIPPING_UPDATE,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            log.info("Created shipping update notification for order: {}, user: {}", orderId, userId);
        } catch (Exception e) {
            log.error("Error processing shipping update event", e);
        }
    }
**/

