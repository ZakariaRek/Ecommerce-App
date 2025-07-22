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
     * Enhanced shipping update listener that handles the new event format from shipping service
     */

    @KafkaListener(topics = "shipping-update", groupId = "${spring.kafka.consumer.group-id}")
    public void listenShippingUpdate(Map<String, Object> shippingUpdate) {
        try {
            log.info("📦 ORDER SERVICE: Received shipping update: {}", shippingUpdate);

            // Extract data directly from the map
            String orderIdStr = (String) shippingUpdate.get("orderId");
            String shippingStatus = (String) shippingUpdate.get("status");
            String shippingId = (String) shippingUpdate.get("shippingId");
            String trackingNumber = (String) shippingUpdate.get("trackingNumber");
            String carrier = (String) shippingUpdate.get("carrier");

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
     * Listen for payment confirmation events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_PAYMENT_CONFIRMED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentConfirmed(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(eventNode.path("orderId").asText());

            // Update order status to PAID
            orderService.updateOrderStatus(orderId, OrderStatus.PAID);

            log.info("📦 ORDER SERVICE: Updated order status to PAID after payment confirmation. Order ID: {}", orderId);
        } catch (EntityNotFoundException e) {
            log.error("📦 ORDER SERVICE: Order not found for payment confirmation event", e);
        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error processing payment confirmation event", e);
        }
    }

    /**
     * Listen for cart checkout events to create orders
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_CART_CHECKED_OUT, groupId = "${spring.kafka.consumer.group-id}")
    public void listenCartCheckedOut(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            String userId = eventNode.path("userId").asText();
            UUID cartId = UUID.fromString(eventNode.path("cartId").asText());

            // Extract shipping and billing address IDs
            UUID billingAddressId = UUID.fromString(eventNode.path("billingAddressId").asText("00000000-0000-0000-0000-000000000000"));
            UUID shippingAddressId = UUID.fromString(eventNode.path("shippingAddressId").asText("00000000-0000-0000-0000-000000000000"));

            // Create new order
            Order newOrder = orderService.createOrder(userId, cartId, billingAddressId, shippingAddressId);

            // Process items from cart (this would typically be more elaborate)
            JsonNode itemsNode = eventNode.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    // Create order items from cart items
                    // Implementation depends on your specific cart item structure
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
    public void listenProductPriceChanged(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID productId = UUID.fromString(eventNode.path("productId").asText());
            BigDecimal newPrice = new BigDecimal(eventNode.path("newPrice").asText());

            // In a real implementation, you would update order items for PENDING orders
            log.info("📦 ORDER SERVICE: Received product price change event. Product ID: {}, New Price: {}",
                    productId, newPrice);
        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error processing product price change event", e);
        }
    }


}