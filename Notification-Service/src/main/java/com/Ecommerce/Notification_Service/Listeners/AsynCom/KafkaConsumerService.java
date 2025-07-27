package com.Ecommerce.Notification_Service.Listeners.AsynCom;

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
 * Enhanced Kafka consumer to handle all product service events and convert them to notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    // ================ PRODUCT EVENTS ================

    @KafkaListener(topics = "product-created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("name").asText();
            String productId = event.path("productId").asText();

            // Notify admin users about new product
            UUID adminUserId = getAdminUserId(); // Implement this method
            notificationService.createNotification(
                    adminUserId,
                    NotificationType.PRODUCT_CREATED,
                    String.format("New product '%s' has been created", productName),
                    LocalDateTime.now().plusDays(7)
            );

            log.info("Created notification for product created: {}", productName);
        } catch (Exception e) {
            log.error("Error processing product created event", e);
        }
    }

    @KafkaListener(topics = "product-updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductUpdated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("name").asText();

            UUID adminUserId = getAdminUserId();
            notificationService.createNotification(
                    adminUserId,
                    NotificationType.PRODUCT_UPDATED,
                    String.format("Product '%s' has been updated", productName),
                    LocalDateTime.now().plusDays(3)
            );

            log.info("Created notification for product updated: {}", productName);
        } catch (Exception e) {
            log.error("Error processing product updated event", e);
        }
    }

    @KafkaListener(topics = "product-price-changed", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductPriceChanged(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("name").asText();
            String previousPrice = event.path("previousPrice").asText();
            String newPrice = event.path("newPrice").asText();

            // Notify interested users about price changes
            notificationService.broadcastSystemNotification(
                    "Price Update",
                    String.format("Price of '%s' changed from %s to %s", productName, previousPrice, newPrice),
                    NotificationType.PRODUCT_PRICE_CHANGED
            );

            log.info("Created notification for product price changed: {}", productName);
        } catch (Exception e) {
            log.error("Error processing product price changed event", e);
        }
    }

    @KafkaListener(topics = "product-status-changed", groupId = "${spring.kafka.consumer.group-id}")
    public void handleProductStatusChanged(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("name").asText();
            String newStatus = event.path("newStatus").asText();

            UUID adminUserId = getAdminUserId();
            notificationService.createNotification(
                    adminUserId,
                    NotificationType.PRODUCT_STATUS_CHANGED,
                    String.format("Product '%s' status changed to %s", productName, newStatus),
                    LocalDateTime.now().plusDays(5)
            );

            log.info("Created notification for product status changed: {}", productName);
        } catch (Exception e) {
            log.error("Error processing product status changed event", e);
        }
    }

    // ================ INVENTORY EVENTS ================

    @KafkaListener(topics = "inventory-low-stock", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryLowStock(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            int currentQuantity = event.path("currentQuantity").asInt();
            int threshold = event.path("lowStockThreshold").asInt();
            String warehouseLocation = event.path("warehouseLocation").asText();

            UUID adminUserId = getAdminUserId();
            notificationService.createInventoryNotification(
                    adminUserId,
                    NotificationType.INVENTORY_LOW_STOCK,
                    productName,
                    currentQuantity,
                    threshold,
                    warehouseLocation
            );

            log.info("Created low stock notification for product: {}", productName);
        } catch (Exception e) {
            log.error("Error processing inventory low stock event", e);
        }
    }

    @KafkaListener(topics = "inventory-restocked", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInventoryRestocked(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            int newQuantity = event.path("newQuantity").asInt();
            String warehouseLocation = event.path("warehouseLocation").asText();

            // Notify users who were interested in this out-of-stock product
            notificationService.broadcastSystemNotification(
                    "Stock Replenished",
                    String.format("'%s' is back in stock! New quantity: %d (Location: %s)",
                            productName, newQuantity, warehouseLocation),
                    NotificationType.INVENTORY_RESTOCKED
            );

            log.info("Created restock notification for product: {}", productName);
        } catch (Exception e) {
            log.error("Error processing inventory restocked event", e);
        }
    }

    // ================ DISCOUNT EVENTS ================

    @KafkaListener(topics = "discount-activated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDiscountActivated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            String discountValue = event.path("discountValue").asText();
            String discountType = event.path("discountType").asText();

            // Notify all users about new discount
            notificationService.broadcastSystemNotification(
                    "New Discount Available",
                    String.format("Special %s discount of %s now available for '%s'!",
                            discountType, discountValue, productName),
                    NotificationType.DISCOUNT_ACTIVATED
            );

            log.info("Created discount activation notification for product: {}", productName);
        } catch (Exception e) {
            log.error("Error processing discount activated event", e);
        }
    }

    @KafkaListener(topics = "discount-expired", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDiscountExpired(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            String discountType = event.path("discountType").asText();

            // Notify users that discount has expired
            notificationService.broadcastSystemNotification(
                    "Discount Expired",
                    String.format("The %s discount for '%s' has expired", discountType, productName),
                    NotificationType.DISCOUNT_EXPIRED
            );

            log.info("Created discount expiration notification for product: {}", productName);
        } catch (Exception e) {
            log.error("Error processing discount expired event", e);
        }
    }

    // ================ CATEGORY EVENTS ================

    @KafkaListener(topics = "category-created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleCategoryCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String categoryName = event.path("name").asText();

            UUID adminUserId = getAdminUserId();
            notificationService.createNotification(
                    adminUserId,
                    NotificationType.CATEGORY_CREATED,
                    String.format("New category '%s' has been created", categoryName),
                    LocalDateTime.now().plusDays(3)
            );

            log.info("Created notification for category created: {}", categoryName);
        } catch (Exception e) {
            log.error("Error processing category created event", e);
        }
    }

    // ================ REVIEW EVENTS ================

    @KafkaListener(topics = "review-created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleReviewCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            int rating = event.path("rating").asInt();

            UUID adminUserId = getAdminUserId();
            notificationService.createNotification(
                    adminUserId,
                    NotificationType.REVIEW_CREATED,
                    String.format("New %d-star review created for '%s'", rating, productName),
                    LocalDateTime.now().plusDays(7)
            );

            log.info("Created notification for review created: {}", productName);
        } catch (Exception e) {
            log.error("Error processing review created event", e);
        }
    }

    @KafkaListener(topics = "review-verified", groupId = "${spring.kafka.consumer.group-id}")
    public void handleReviewVerified(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String productName = event.path("productName").asText();
            UUID userId = UUID.fromString(event.path("userId").asText());

            notificationService.createNotification(
                    userId,
                    NotificationType.REVIEW_VERIFIED,
                    String.format("Your review for '%s' has been verified", productName),
                    LocalDateTime.now().plusDays(30)
            );

            log.info("Created notification for review verified: {}", productName);
        } catch (Exception e) {
            log.error("Error processing review verified event", e);
        }
    }

    // ================ SUPPLIER EVENTS ================

    @KafkaListener(topics = "supplier-created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleSupplierCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String supplierName = event.path("name").asText();

            UUID adminUserId = getAdminUserId();
            notificationService.createNotification(
                    adminUserId,
                    NotificationType.SUPPLIER_CREATED,
                    String.format("New supplier '%s' has been added", supplierName),
                    LocalDateTime.now().plusDays(7)
            );

            log.info("Created notification for supplier created: {}", supplierName);
        } catch (Exception e) {
            log.error("Error processing supplier created event", e);
        }
    }

    // ================ EXISTING EVENTS (Enhanced) ================

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

    // ================ HELPER METHODS ================

    /**
     * Get admin user ID - implement based on your user management system
     */
    private UUID getAdminUserId() {
        // TODO: Implement this based on your user management
        // For now, returning a fixed UUID - replace with actual admin user lookup
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}