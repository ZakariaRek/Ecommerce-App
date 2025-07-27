package com.Ecommerce.Notification_Service.Listeners.AsynCom;

import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Payload.Kafka.UserInfoResponse;
import com.Ecommerce.Notification_Service.Services.EmailService;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import com.Ecommerce.Notification_Service.Services.UserEmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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

    // ================ PAYMENT SERVICE EVENTS ================


    private UUID parseOrConvertToUUID(String userId) {
        try {
            // Try to parse as UUID first
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // If not a UUID, convert from MongoDB ObjectId using REVERSIBLE method
            return convertObjectIdToUuidReversible(userId);
        }
    }

    /**
     * Convert MongoDB ObjectId to UUID using a REVERSIBLE approach
     * This allows the User Service to convert back to original ObjectId
     */
    private UUID convertObjectIdToUuidReversible(String objectId) {
        try {
            // Validate ObjectId format (24 hex characters)
            if (objectId == null || !objectId.matches("^[0-9a-fA-F]{24}$")) {
                throw new IllegalArgumentException("Invalid ObjectId format: " + objectId);
            }

            // Convert ObjectId to bytes (12 bytes from 24 hex chars)
            byte[] objectIdBytes = hexStringToBytes(objectId);

            // Pad to 16 bytes for UUID (add 4 zero bytes)
            byte[] uuidBytes = new byte[16];
            System.arraycopy(objectIdBytes, 0, uuidBytes, 0, 12);
            // Last 4 bytes remain zero as padding

            // Create UUID from bytes
            long mostSigBits = 0;
            long leastSigBits = 0;

            for (int i = 0; i < 8; i++) {
                mostSigBits = (mostSigBits << 8) | (uuidBytes[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                leastSigBits = (leastSigBits << 8) | (uuidBytes[i] & 0xff);
            }

            UUID convertedUuid = new UUID(mostSigBits, leastSigBits);
            log.debug("Converted ObjectId {} to UUID {}", objectId, convertedUuid);

            return convertedUuid;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ObjectId format: " + objectId, e);
        }
    }

    /**
     * Convert hex string to bytes
     */
    private byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }




// ================ PAYMENT SERVICE EVENTS ================

    @KafkaListener(topics = "payment-confirmed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentConfirmed(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
            String orderId = event.path("orderId").asText();
            String amount = event.path("amount").asText();
            String paymentMethod = event.path("paymentMethod").asText("Credit Card");

            String content = String.format("Payment of %s has been confirmed for order #%s. Thank you for your purchase!", amount, orderId);
            log.info("id user  :-----------" + userId);

            // Create in-app notification
//            notificationService.createNotification(
//                    userId,
//                    NotificationType.PAYMENT_CONFIRMATION,
//                    content,
//                    LocalDateTime.now().plusDays(7)
//            );

            // Send enhanced email notification with user information
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createPaymentTemplateData(orderId, amount, paymentMethod, userInfo);
                emailService.sendPaymentConfirmationEmailWithUserInfo(userInfo, orderId, amount, paymentMethod, templateData);
            }, "payment confirmation");

            log.info("Processed payment confirmation notification. Order: {}, User: {}, Amount: {}", orderId, userId, amount);

        } catch (Exception e) {
            log.error("Error processing payment confirmation event", e);
        }
    }

    @KafkaListener(topics = "payment-failed", groupId = "order-service-group")
    public void handlePaymentFailed(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
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

            // Send enhanced email notification
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createPaymentFailureTemplateData(orderId, reason, userInfo);
                emailService.sendEmailToUserWithInfo(userInfo, "❌ Payment Failed - Order #" + orderId, content, NotificationType.PAYMENT_CONFIRMATION, templateData);
            }, "payment failure");

            log.info("Processed payment failed notification. Order: {}, User: {}", orderId, userId);

        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
        }
    }

    @KafkaListener(topics = "payment-updated", groupId = "$order-service-group")
    public void handlePaymentUpdated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
            String orderId = event.path("orderId").asText();
            String paymentId = event.path("paymentId").asText();
            String status = event.path("status").asText();
            String eventMessage = event.path("message").asText("Payment status updated");

            String content = String.format("Payment update for order #%s: %s", orderId, eventMessage);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_CONFIRMATION,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send enhanced email notification for important updates (like refunds)
            if (status.toLowerCase().contains("refund")) {
                sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                    Map<String, Object> templateData = createPaymentUpdateTemplateData(orderId, paymentId, status, eventMessage, userInfo);
                    emailService.sendEmailToUserWithInfo(userInfo, "💰 Payment Update - Order #" + orderId, content, NotificationType.PAYMENT_CONFIRMATION, templateData);
                }, "payment update");
            }

            log.info("Processed payment update notification. Order: {}, User: {}, Status: {}", orderId, userId, status);

        } catch (Exception e) {
            log.error("Error processing payment update event", e);
        }
    }

// ================ SHIPPING SERVICE EVENTS ================

    @KafkaListener(topics = "shipping-created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleShippingCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
            String orderId = event.path("orderId").asText();
            String shippingId = event.path("shippingId").asText();
            String carrier = event.path("carrier").asText("Standard Carrier");

            String content = String.format("Shipping has been arranged for order #%s with %s. You'll receive tracking information soon.", orderId, carrier);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.SHIPPING_UPDATE,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send shipping creation email
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createShippingCreatedTemplateData(orderId, shippingId, carrier, userInfo);
                emailService.sendEmailToUserWithInfo(userInfo, "📦 Shipping Arranged - Order #" + orderId, content, NotificationType.SHIPPING_UPDATE, templateData);
            }, "shipping created");

            log.info("Processed shipping created notification. Order: {}, User: {}, ShippingId: {}", orderId, userId, shippingId);

        } catch (Exception e) {
            log.error("Error processing shipping created event", e);
        }
    }

    @KafkaListener(topics = "shipping-update", groupId = "${spring.kafka.consumer.group-id}")
    public void handleShippingUpdate(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
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

            // Send enhanced shipping update email with user address information
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createShippingTemplateData(orderId, status, trackingNumber, userInfo);
                emailService.sendShippingUpdateEmailWithUserInfo(userInfo, orderId, status, trackingNumber, templateData);
            }, "shipping update");

            log.info("Processed shipping update notification. Order: {}, User: {}, Status: {}", orderId, userId, status);

        } catch (Exception e) {
            log.error("Error processing shipping update event", e);
        }
    }

    @KafkaListener(topics = "shipping-delivered", groupId = "${spring.kafka.consumer.group-id}")
    public void handleShippingDelivered(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
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

            // Send delivery confirmation with user address
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createDeliveryTemplateData(orderId, deliveryDate, userInfo);
                emailService.sendDeliveryConfirmationEmailWithUserInfo(userInfo, orderId, deliveryDate, templateData);
            }, "delivery confirmation");

            log.info("Processed delivery notification. Order: {}, User: {}", orderId, userId);

        } catch (Exception e) {
            log.error("Error processing delivery event", e);
        }
    }

// ================ ORDER SERVICE EVENTS ================

    @KafkaListener(topics = "order-status-changed", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderStatusChanged(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
            String orderId = event.path("orderId").asText();
            String oldStatus = event.path("oldStatus").asText("");
            String newStatus = event.path("newStatus").asText();

            String content = String.format("Your order #%s status has been updated to: %s", orderId, newStatus);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.ORDER_STATUS,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send email for important order status changes
            if (isImportantOrderStatus(newStatus)) {
                sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                    Map<String, Object> templateData = createOrderStatusTemplateData(orderId, oldStatus, newStatus, userInfo);
                    emailService.sendEmailToUserWithInfo(userInfo, "📋 Order Status Update - Order #" + orderId, content, NotificationType.ORDER_STATUS, templateData);
                }, "order status change");
            }

            log.info("Processed order status change notification. Order: {}, User: {}, Status: {} -> {}", orderId, userId, oldStatus, newStatus);

        } catch (Exception e) {
            log.error("Error processing order status change event", e);
        }
    }

    @KafkaListener(topics = "order-cancelled", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCancelled(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = parseOrConvertToUUID(event.path("userId").asText()); // FIXED: Use helper method
            String orderId = event.path("orderId").asText();
            String reason = event.path("reason").asText("Order cancelled");

            String content = String.format("Your order #%s has been cancelled. Reason: %s", orderId, reason);

            // Create in-app notification
            notificationService.createNotification(
                    userId,
                    NotificationType.ORDER_STATUS,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send cancellation email
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createOrderCancellationTemplateData(orderId, reason, userInfo);
                emailService.sendEmailToUserWithInfo(userInfo, "❌ Order Cancelled - Order #" + orderId, content, NotificationType.ORDER_STATUS, templateData);
            }, "order cancellation");

            log.info("Processed order cancellation notification. Order: {}, User: {}", orderId, userId);

        } catch (Exception e) {
            log.error("Error processing order cancellation event", e);
        }
    }

// Continue with other event handlers following the same pattern...
// All other methods that use UUID.fromString(event.path("userId").asText()) need to be updated similarly
    // ================ INVENTORY SERVICE EVENTS ================

    @KafkaListener(topics = "inventory-low-stock", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryLowStock(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            int currentQuantity = event.path("currentQuantity").asInt();
            String productId = event.path("productId").asText("");

            // This is typically sent to admin users, but we can also notify users who have this item in wishlist
            String content = String.format("Limited stock alert: Only %d units left of %s", currentQuantity, productName);

            // Broadcast to interested users (you could maintain a wishlist/interest list)
            notificationService.broadcastSystemNotification(
                    "Low Stock Alert",
                    content,
                    NotificationType.INVENTORY_LOW_STOCK
            );

            log.info("Processed low stock notification for product: {}", productName);

        } catch (Exception e) {
            log.error("Error processing low stock event", e);
        }
    }

    @KafkaListener(topics = "inventory-restocked", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryRestocked(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            int newQuantity = event.path("newQuantity").asInt();
            String productId = event.path("productId").asText("");

            String content = String.format("Good news! %s is back in stock with %d units available", productName, newQuantity);

            // Broadcast to interested users
            notificationService.broadcastSystemNotification(
                    "Back in Stock",
                    content,
                    NotificationType.INVENTORY_RESTOCKED
            );

            log.info("Processed restock notification for product: {}", productName);

        } catch (Exception e) {
            log.error("Error processing restock event", e);
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

            // Send personalized loyalty points email
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createLoyaltyTemplateData(pointsEarned, totalPoints, reason, userInfo);
                emailService.sendLoyaltyPointsEmailWithUserInfo(userInfo, pointsEarned, totalPoints, reason, templateData);
            }, "loyalty points earned");

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

            // Send personalized tier upgrade email
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createTierUpgradeTemplateData(newTier, oldTier, userInfo);
                emailService.sendTierUpgradeEmailWithUserInfo(userInfo, newTier, oldTier, templateData);
            }, "loyalty tier upgrade");

            log.info("Processed loyalty tier upgrade notification. User: {}, New Tier: {}", userId, newTier);

        } catch (Exception e) {
            log.error("Error processing loyalty tier upgrade event", e);
        }
    }

    // ================ PROMOTION EVENTS ================

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

                // Send enhanced bulk promotional emails with user information
                sendBulkEnhancedEmailNotificationAsync(targetUserIds, (userInfoMap) -> {
                    return emailService.sendBulkPromotionalEmailWithUserInfo(userInfoMap, promotionTitle, promotionMessage, promoCode, validUntil);
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

    @KafkaListener(topics = "discount-activated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDiscountActivated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            String discountValue = event.path("discountValue").asText();
            String discountType = event.path("discountType").asText();
            String validUntil = event.path("validUntil").asText("");

            String content = String.format("Special %s discount of %s now available for '%s'!", discountType, discountValue, productName);

            // Broadcast discount notification
            notificationService.broadcastSystemNotification(
                    "New Discount Available",
                    content,
                    NotificationType.DISCOUNT_ACTIVATED
            );

            log.info("Processed discount activation notification for product: {}", productName);

        } catch (Exception e) {
            log.error("Error processing discount activated event", e);
        }
    }

    // ================ USER ACCOUNT EVENTS ================

    @KafkaListener(topics = "user-registered", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserRegistered(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());
            String email = event.path("email").asText();
            String firstName = event.path("firstName").asText("");

            String content = String.format("Welcome to our platform%s! Your account has been successfully created.",
                    firstName.isEmpty() ? "" : ", " + firstName);

            // Create welcome notification
            notificationService.createNotification(
                    userId,
                    NotificationType.ACCOUNT_ACTIVITY,
                    content,
                    LocalDateTime.now().plusDays(30)
            );

            // Send welcome email
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createWelcomeTemplateData(userInfo);
                emailService.sendEmailToUserWithInfo(userInfo, "🎉 Welcome to Our Platform!", content, NotificationType.ACCOUNT_ACTIVITY, templateData);
            }, "user registration");

            log.info("Processed user registration notification. User: {}", userId);

        } catch (Exception e) {
            log.error("Error processing user registration event", e);
        }
    }

    @KafkaListener(topics = "password-changed", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePasswordChanged(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID userId = UUID.fromString(event.path("userId").asText());

            String content = "Your password has been successfully changed. If you didn't make this change, please contact support immediately.";

            // Create security notification
            notificationService.createNotification(
                    userId,
                    NotificationType.ACCOUNT_ACTIVITY,
                    content,
                    LocalDateTime.now().plusDays(7)
            );

            // Send security email
            sendEnhancedEmailNotificationAsync(userId, (userInfo) -> {
                Map<String, Object> templateData = createSecurityTemplateData("Password Changed", userInfo);
                emailService.sendEmailToUserWithInfo(userInfo, "🔐 Password Changed", content, NotificationType.ACCOUNT_ACTIVITY, templateData);
            }, "password changed");

            log.info("Processed password change notification. User: {}", userId);

        } catch (Exception e) {
            log.error("Error processing password change event", e);
        }
    }

    // ================ EMAIL HELPER METHODS ================

    /**
     * Send enhanced email notification with full user information
     */
    private void sendEnhancedEmailNotificationAsync(UUID userId, EnhancedEmailSender emailSender, String purpose) {
        userEmailService.getUserInfo(userId, purpose.toUpperCase(), true, false)
                .thenAccept(userInfo -> {
                    if (userInfo != null && "SUCCESS".equals(userInfo.getStatus_response()) && userInfo.getEmail() != null) {
                        try {
                            emailSender.sendEmail(userInfo);
                            log.debug("Enhanced email sent successfully for {}: {} to {}", purpose, userInfo.getFullName(), userInfo.getEmail());
                        } catch (Exception e) {
                            log.error("Failed to send enhanced email for {}: {}", purpose, userInfo.getEmail(), e);
                        }
                    } else {
                        log.warn("No user information available for user {} for {}", userId, purpose);
                        // Fallback to basic email service
                        sendBasicEmailFallback(userId, purpose);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Failed to fetch user info for user {} for {}", userId, purpose, ex);
                    sendBasicEmailFallback(userId, purpose);
                    return null;
                });
    }

    /**
     * Send bulk enhanced email notification with full user information
     */
    private void sendBulkEnhancedEmailNotificationAsync(List<UUID> userIds, BulkEnhancedEmailSender emailSender, String purpose) {
        userEmailService.getBulkUserInfo(userIds, purpose.toUpperCase(), true, false)
                .thenAccept(userInfoMap -> {
                    if (!userInfoMap.isEmpty()) {
                        try {
                            emailSender.sendBulkEmail(userInfoMap);
                            log.debug("Bulk enhanced email sent successfully for {}: {} recipients", purpose, userInfoMap.size());
                        } catch (Exception e) {
                            log.error("Failed to send bulk enhanced email for {}: {} recipients", purpose, userInfoMap.size(), e);
                        }
                    } else {
                        log.warn("No user information available for {} users for {}", userIds.size(), purpose);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Failed to fetch user info for {} users for {}", userIds.size(), purpose, ex);
                    return null;
                });
    }

    /**
     * Fallback to basic email service when user info is not available
     */
    private void sendBasicEmailFallback(UUID userId, String purpose) {
        userEmailService.getUserEmailWithFallback(userId)
                .thenAccept(email -> {
                    if (email != null && !email.isEmpty()) {
                        try {
                            // Send basic notification email
                            String subject = "Notification - " + purpose;
                            String content = "You have a new notification. Please check your account for details.";
                            emailService.sendEmailToUser(email, subject, content, NotificationType.SYSTEM_ALERT);
                            log.debug("Fallback email sent to: {}", email);
                        } catch (Exception e) {
                            log.error("Failed to send fallback email to: {}", email, e);
                        }
                    }
                });
    }

    // ================ HELPER METHODS FOR STATUS CHECKING ================

    private boolean isSignificantStatusChange(String status) {
        return status.toLowerCase().contains("shipped") ||
                status.toLowerCase().contains("out_for_delivery") ||
                status.toLowerCase().contains("delivered") ||
                status.toLowerCase().contains("exception");
    }

    private boolean isImportantOrderStatus(String status) {
        return status.toLowerCase().contains("confirmed") ||
                status.toLowerCase().contains("processing") ||
                status.toLowerCase().contains("shipped") ||
                status.toLowerCase().contains("cancelled") ||
                status.toLowerCase().contains("completed");
    }

    // ================ TEMPLATE DATA CREATORS ================

    private Map<String, Object> createPaymentTemplateData(String orderId, String amount, String paymentMethod, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("amount", amount);
        data.put("paymentMethod", paymentMethod);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        if (userInfo.getDefaultAddress() != null) {
            data.put("shippingAddress", userInfo.getFormattedDefaultAddress());
        }
        return data;
    }

    private Map<String, Object> createPaymentFailureTemplateData(String orderId, String reason, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("reason", reason);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        return data;
    }

    private Map<String, Object> createPaymentUpdateTemplateData(String orderId, String paymentId, String status, String message, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("paymentId", paymentId);
        data.put("status", status);
        data.put("message", message);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        return data;
    }

    private Map<String, Object> createShippingCreatedTemplateData(String orderId, String shippingId, String carrier, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("shippingId", shippingId);
        data.put("carrier", carrier);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        if (userInfo.getDefaultAddress() != null) {
            data.put("shippingAddress", userInfo.getFormattedDefaultAddress());
        }
        return data;
    }

    private Map<String, Object> createShippingTemplateData(String orderId, String status, String trackingNumber, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("status", status);
        data.put("trackingNumber", trackingNumber);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        if (userInfo.getDefaultAddress() != null) {
            data.put("deliveryAddress", userInfo.getFormattedDefaultAddress());
            data.put("city", userInfo.getDefaultAddress().getCity());
            data.put("state", userInfo.getDefaultAddress().getState());
        }
        return data;
    }

    private Map<String, Object> createShippingStatusChangeTemplateData(String orderId, String oldStatus, String newStatus, String location, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("oldStatus", oldStatus);
        data.put("newStatus", newStatus);
        data.put("location", location);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        if (userInfo.getDefaultAddress() != null) {
            data.put("deliveryAddress", userInfo.getFormattedDefaultAddress());
        }
        return data;
    }

    private Map<String, Object> createDeliveryTemplateData(String orderId, String deliveryDate, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("deliveryDate", deliveryDate);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        if (userInfo.getDefaultAddress() != null) {
            data.put("deliveryAddress", userInfo.getFormattedDefaultAddress());
        }
        return data;
    }

    private Map<String, Object> createOrderStatusTemplateData(String orderId, String oldStatus, String newStatus, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("oldStatus", oldStatus);
        data.put("newStatus", newStatus);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        return data;
    }

    private Map<String, Object> createOrderCancellationTemplateData(String orderId, String reason, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("reason", reason);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        return data;
    }

    private Map<String, Object> createLoyaltyTemplateData(int pointsEarned, int totalPoints, String reason, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("pointsEarned", pointsEarned);
        data.put("totalPoints", totalPoints);
        data.put("reason", reason);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        return data;
    }

    private Map<String, Object> createTierUpgradeTemplateData(String newTier, String oldTier, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("newTier", newTier);
        data.put("oldTier", oldTier);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        return data;
    }

    private Map<String, Object> createWelcomeTemplateData(UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        data.put("joinDate", LocalDateTime.now().toLocalDate().toString());
        return data;
    }

    private Map<String, Object> createSecurityTemplateData(String actionType, UserInfoResponse userInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("actionType", actionType);
        data.put("userName", userInfo.getFullName());
        data.put("userEmail", userInfo.getEmail());
        data.put("timestamp", LocalDateTime.now().toString());
        return data;
    }

    // ================ FUNCTIONAL INTERFACES ================

    @FunctionalInterface
    private interface EnhancedEmailSender {
        void sendEmail(UserInfoResponse userInfo) throws Exception;
    }

    @FunctionalInterface
    private interface BulkEnhancedEmailSender {
        CompletableFuture<Void> sendBulkEmail(Map<UUID, UserInfoResponse> userInfoMap) throws Exception;
    }
}