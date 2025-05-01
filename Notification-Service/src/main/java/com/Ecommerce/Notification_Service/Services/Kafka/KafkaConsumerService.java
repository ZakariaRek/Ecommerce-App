package com.Ecommerce.Notification_Service.Services.Kafka;

import com.Ecommerce.Notification_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for consuming events from Kafka topics from other services
 * and creating notifications based on those events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Listen for order status change events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_ORDER_STATUS_CHANGED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenOrderStatusChanged(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID userId = UUID.fromString(eventNode.path("userId").asText());
            String orderId = eventNode.path("orderId").asText();
            String newStatus = eventNode.path("newStatus").asText();

            String content = String.format("Your order #%s has been updated to status: %s", orderId, newStatus);

            // Create notification for the user
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

    /**
     * Listen for payment confirmation events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_PAYMENT_CONFIRMED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentConfirmed(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID userId = UUID.fromString(eventNode.path("userId").asText());
            String orderId = eventNode.path("orderId").asText();
            String amount = eventNode.path("amount").asText();

            String content = String.format("Payment of %s has been confirmed for order #%s. Thank you for your purchase!", amount, orderId);

            // Create notification for the user
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

    /**
     * Listen for product restock events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_PRODUCT_RESTOCKED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenProductRestocked(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID productId = UUID.fromString(eventNode.path("productId").asText());
            String productName = eventNode.path("productName").asText();
            JsonNode interestedUsersNode = eventNode.path("interestedUsers");

            if (interestedUsersNode.isArray()) {
                for (JsonNode userNode : interestedUsersNode) {
                    UUID userId = UUID.fromString(userNode.asText());
                    String content = String.format("Good news! %s is back in stock. Get it before it's gone again!", productName);

                    // Create notification for each interested user
                    notificationService.createNotification(
                            userId,
                            NotificationType.PRODUCT_RESTOCKED,
                            content,
                            LocalDateTime.now().plusDays(3)
                    );
                }

                log.info("Created restock notifications for product: {}", productName);
            }
        } catch (Exception e) {
            log.error("Error processing product restock event", e);
        }
    }

    /**
     * Listen for price drop events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_PRICE_DROP, groupId = "${spring.kafka.consumer.group-id}")
    public void listenPriceDrop(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID productId = UUID.fromString(eventNode.path("productId").asText());
            String productName = eventNode.path("productName").asText();
            String oldPrice = eventNode.path("oldPrice").asText();
            String newPrice = eventNode.path("newPrice").asText();
            JsonNode interestedUsersNode = eventNode.path("interestedUsers");

            if (interestedUsersNode.isArray()) {
                for (JsonNode userNode : interestedUsersNode) {
                    UUID userId = UUID.fromString(userNode.asText());
                    String content = String.format("Price drop alert! %s is now %s (was %s). Grab it while it's on sale!",
                            productName, newPrice, oldPrice);

                    // Create notification for each interested user
                    notificationService.createNotification(
                            userId,
                            NotificationType.PRICE_DROP,
                            content,
                            LocalDateTime.now().plusDays(3)
                    );
                }

                log.info("Created price drop notifications for product: {}", productName);
            }
        } catch (Exception e) {
            log.error("Error processing price drop event", e);
        }
    }

    /**
     * Listen for cart abandoned events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_CART_ABANDONED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenCartAbandoned(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID userId = UUID.fromString(eventNode.path("userId").asText());
            int itemCount = eventNode.path("itemCount").asInt();

            String content = String.format("You have %d items waiting in your cart. Complete your purchase now to avoid missing out!", itemCount);

            // Create notification for the user after a delay (e.g., 1 day)
            // In a real implementation, this might be scheduled rather than immediate
            notificationService.createNotification(
                    userId,
                    NotificationType.ACCOUNT_ACTIVITY,
                    content,
                    LocalDateTime.now().plusDays(3)
            );

            log.info("Created cart abandoned notification for user: {}", userId);
        } catch (Exception e) {
            log.error("Error processing cart abandoned event", e);
        }
    }

    /**
     * Listen for shipping update events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_SHIPPING_UPDATE, groupId = "${spring.kafka.consumer.group-id}")
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

            // Create notification for the user
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
}