// Fixed Order-Service/src/main/java/com/Ecommerce/Order_Service/Listeners/AsyncComm/KafkaStatusLisiner.java

package com.Ecommerce.Order_Service.Listeners.AsyncComm;

import com.Ecommerce.Order_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced service for consuming events from Kafka topics from other services
 * and processing them for the Order Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaStatusLisiner {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    // DateTimeFormatter for parsing timestamps
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * REMOVED: Payment confirmed listener to avoid conflicts with EnhancedKafkaConsumerService
     * Let EnhancedKafkaConsumerService handle payment events exclusively
     */

    /**
     * Enhanced shipping update listener that handles the new event format from shipping service
     */
    @KafkaListener(topics = "shipping-update", groupId = "${spring.kafka.consumer.group-id}")
    public void listenShippingUpdate(Map<String, Object> shippingUpdate) {
        try {
            log.info("📦 ORDER SERVICE: Received shipping update: {}", shippingUpdate);

            // Extract data directly from the map
            String orderIdStr = getStringValue(shippingUpdate, "orderId");
            String shippingStatus = getStringValue(shippingUpdate, "status");
            String shippingId = getStringValue(shippingUpdate, "shippingId");
            String trackingNumber = getStringValue(shippingUpdate, "trackingNumber");
            String carrier = getStringValue(shippingUpdate, "carrier");

            if (orderIdStr == null || orderIdStr.isEmpty()) {
                log.error("📦 ORDER SERVICE: Missing orderId in shipping update event");
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr);

            // Map shipping status to order status
            OrderStatus orderStatus = mapShippingStatusToOrderStatus(shippingStatus);

            if (orderStatus == null) {
                log.warn("📦 ORDER SERVICE: Cannot map shipping status '{}' to order status", shippingStatus);
                return;
            }

            // Update order status
            Order updatedOrder = orderService.updateOrderStatus(orderId, orderStatus);

            log.info("📦 ORDER SERVICE: Updated order {} status from shipping {} to order status: {} (shipping status: {})",
                    orderId, shippingId, orderStatus, shippingStatus);

            // Log additional shipping information if available
            if (trackingNumber != null && !trackingNumber.isEmpty()) {
                log.info("📦 ORDER SERVICE: Order {} tracking number: {}", orderId, trackingNumber);
            }

            if (carrier != null && !carrier.isEmpty()) {
                log.info("📦 ORDER SERVICE: Order {} carrier: {}", orderId, carrier);
            }

            // Handle delivery date if present
            Object deliveredDate = shippingUpdate.get("deliveredDate");
            if (deliveredDate != null) {
                log.info("📦 ORDER SERVICE: Order {} delivered at: {}", orderId, deliveredDate);
            }

        } catch (EntityNotFoundException e) {
            log.error("📦 ORDER SERVICE: Order not found for shipping update event", e);
        } catch (IllegalArgumentException e) {
            log.error("📦 ORDER SERVICE: Invalid data in shipping update event", e);
        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error processing shipping update event", e);
        }
    }

    /**
     * Maps shipping status to corresponding order status
     */
    private OrderStatus mapShippingStatusToOrderStatus(String shippingStatus) {
        if (shippingStatus == null || shippingStatus.isEmpty()) {
            return null;
        }

        try {
            switch (shippingStatus.toUpperCase()) {
                case "PENDING":
                    return OrderStatus.PENDING;

                case "PREPARING":
                    return OrderStatus.PROCESSING;

                case "SHIPPED":
                case "IN_TRANSIT":
                    return OrderStatus.SHIPPED;

                case "OUT_FOR_DELIVERY":
                    return OrderStatus.SHIPPED; // Or create a new OUT_FOR_DELIVERY status

                case "DELIVERED":
                    return OrderStatus.DELIVERED;

                case "FAILED":
                case "RETURNED":
                    return OrderStatus.CANCELED; // Or handle differently based on business logic

                default:
                    log.warn("📦 ORDER SERVICE: Unknown shipping status: {}", shippingStatus);
                    return null;
            }
        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error mapping shipping status '{}' to order status", shippingStatus, e);
            return null;
        }
    }

    /**
     * Listen for cart checkout events to create orders
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_CART_CHECKED_OUT, groupId = "${spring.kafka.consumer.group-id}")
    public void listenCartCheckedOut(Map<String, Object> cartEvent) {
        try {
            String userId = getStringValue(cartEvent, "userId");
            String cartIdStr = getStringValue(cartEvent, "cartId");
            UUID cartId = UUID.fromString(cartIdStr);

            // Extract shipping and billing address IDs
            String billingAddressIdStr = getStringValue(cartEvent, "billingAddressId");
            String shippingAddressIdStr = getStringValue(cartEvent, "shippingAddressId");

            UUID billingAddressId = billingAddressIdStr.isEmpty() ?
                    UUID.fromString("00000000-0000-0000-0000-000000000000") : UUID.fromString(billingAddressIdStr);
            UUID shippingAddressId = shippingAddressIdStr.isEmpty() ?
                    UUID.fromString("00000000-0000-0000-0000-000000000000") : UUID.fromString(shippingAddressIdStr);

            // Create new order
            Order newOrder = orderService.createOrder(userId, cartId, billingAddressId, shippingAddressId);

            // Process items from cart if available
            Object itemsObj = cartEvent.get("items");
            if (itemsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) itemsObj;

                for (Map<String, Object> itemMap : items) {
                    // Create order items from cart items
                    log.info("📦 ORDER SERVICE: Processing cart item for order {}", newOrder.getId());
                }
            }

            log.info("📦 ORDER SERVICE: Created new order from cart checkout event. Order ID: {}, User ID: {}",
                    newOrder.getId(), userId);
        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error processing cart checkout event", e);
        }
    }

    /**
     * Listen for product price changes to update orders that are still PENDING
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_PRODUCT_PRICE_CHANGED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenProductPriceChanged(Map<String, Object> priceEvent) {
        try {
            String productIdStr = getStringValue(priceEvent, "productId");
            UUID productId = UUID.fromString(productIdStr);

            Double newPriceDouble = getDoubleValue(priceEvent, "newPrice");
            BigDecimal newPrice = BigDecimal.valueOf(newPriceDouble);

            // In a real implementation, you would update order items for PENDING orders
            log.info("📦 ORDER SERVICE: Received product price change event. Product ID: {}, New Price: {}",
                    productId, newPrice);
        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error processing product price change event", e);
        }
    }

    // Helper methods
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("📦 ORDER SERVICE: Could not parse '{}' as double for key '{}'", value, key);
            }
        }
        return 0.0;
    }
}