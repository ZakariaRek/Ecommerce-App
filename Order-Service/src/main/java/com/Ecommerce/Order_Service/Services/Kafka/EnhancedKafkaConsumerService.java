// Order-Service/src/main/java/com/Ecommerce/Order_Service/Services/Kafka/EnhancedKafkaConsumerService.java
package com.Ecommerce.Order_Service.Services.Kafka;

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

import java.util.UUID;

/**
 * Enhanced service for consuming payment events from Kafka topics 
 * and processing them for the Order Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedKafkaConsumerService {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * Listen for payment confirmed events from Payment Service
     */
    @KafkaListener(topics = "payment-confirmed", groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentConfirmed(String message) {
        try {
            log.info("💳 ORDER SERVICE: Received payment confirmed event: {}", message);

            JsonNode eventNode = objectMapper.readTree(message);

            // Extract order ID from the payment event
            String orderIdStr = extractOrderIdFromEvent(eventNode);
            if (orderIdStr == null) {
                log.warn("💳 ORDER SERVICE: No order ID found in payment confirmed event");
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr);

            // Check if payment was successful
            boolean paymentSuccess = extractPaymentSuccess(eventNode);

            if (paymentSuccess) {
                // Update order status to PAID
                orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                log.info("💳 ORDER SERVICE: Updated order {} status to PAID after payment confirmation", orderId);
            } else {
                // Update order status to PAYMENT_FAILED
                orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
                log.warn("💳 ORDER SERVICE: Updated order {} status to PAYMENT_FAILED", orderId);
            }

        } catch (EntityNotFoundException e) {
            log.error("💳 ORDER SERVICE: Order not found for payment confirmation event", e);
        } catch (Exception e) {
            log.error("💳 ORDER SERVICE: Error processing payment confirmation event", e);
        }
    }

    /**
     * Listen for payment created events
     */
    @KafkaListener(topics = "payment-created", groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentCreated(String message) {
        try {
            log.info("💳 ORDER SERVICE: Received payment created event: {}", message);

            JsonNode eventNode = objectMapper.readTree(message);
            String orderIdStr = extractOrderIdFromEvent(eventNode);

            if (orderIdStr != null) {
                UUID orderId = UUID.fromString(orderIdStr);
                log.info("💳 ORDER SERVICE: Payment created for order: {}", orderId);

                // Optionally update order status to indicate payment is being processed
                // orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING);
            }

        } catch (Exception e) {
            log.error("💳 ORDER SERVICE: Error processing payment created event", e);
        }
    }

    /**
     * Listen for payment updated events
     */
    @KafkaListener(topics = "payment-updated", groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentUpdated(String message) {
        try {
            log.info("💳 ORDER SERVICE: Received payment updated event: {}", message);

            JsonNode eventNode = objectMapper.readTree(message);
            String orderIdStr = extractOrderIdFromEvent(eventNode);
            String paymentStatus = extractPaymentStatus(eventNode);

            if (orderIdStr != null && paymentStatus != null) {
                UUID orderId = UUID.fromString(orderIdStr);

                // Update order status based on payment status
                switch (paymentStatus.toUpperCase()) {
                    case "COMPLETED":
                        orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                        log.info("💳 ORDER SERVICE: Updated order {} to PAID (payment completed)", orderId);
                        break;
                    case "FAILED":
                        orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
                        log.warn("💳 ORDER SERVICE: Updated order {} to PAYMENT_FAILED", orderId);
                        break;
                    case "REFUNDED":
                        orderService.updateOrderStatus(orderId, OrderStatus.REFUNDED);
                        log.info("💳 ORDER SERVICE: Updated order {} to REFUNDED", orderId);
                        break;
                    default:
                        log.info("💳 ORDER SERVICE: Payment status {} for order {} - no order status change needed",
                                paymentStatus, orderId);
                }
            }

        } catch (Exception e) {
            log.error("💳 ORDER SERVICE: Error processing payment updated event", e);
        }
    }

    /**
     * Listen for payment failed events
     */
    @KafkaListener(topics = "payment-failed", groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentFailed(String message) {
        try {
            log.warn("💳 ORDER SERVICE: Received payment failed event: {}", message);

            JsonNode eventNode = objectMapper.readTree(message);
            String orderIdStr = extractOrderIdFromEvent(eventNode);

            if (orderIdStr != null) {
                UUID orderId = UUID.fromString(orderIdStr);

                // Update order status to PAYMENT_FAILED
                orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
                log.warn("💳 ORDER SERVICE: Updated order {} status to PAYMENT_FAILED", orderId);
            }

        } catch (EntityNotFoundException e) {
            log.error("💳 ORDER SERVICE: Order not found for payment failed event", e);
        } catch (Exception e) {
            log.error("💳 ORDER SERVICE: Error processing payment failed event", e);
        }
    }

    /**
     * Extract order ID from various payment event formats
     */
    private String extractOrderIdFromEvent(JsonNode eventNode) {
        // Try different possible paths for order ID
        String[] possiblePaths = {
                "orderId", "order_id", "OrderID",
                "data.orderId", "data.order_id", "data.OrderID",
                "Data.orderId", "Data.order_id", "Data.OrderID"
        };

        for (String path : possiblePaths) {
            JsonNode node = eventNode.at("/" + path.replace(".", "/"));
            if (!node.isMissingNode() && !node.isNull()) {
                return node.asText();
            }
        }

        // Fallback: look for any field containing "order" and "id"
        return findFieldContaining(eventNode, "order", "id");
    }

    /**
     * Extract payment success status from event
     */
    private boolean extractPaymentSuccess(JsonNode eventNode) {
        // Try different possible paths for success status
        String[] successPaths = {
                "success", "Success", "successful", "Successful",
                "data.success", "data.Success", "data.successful",
                "Data.success", "Data.Success", "Data.successful"
        };

        for (String path : successPaths) {
            JsonNode node = eventNode.at("/" + path.replace(".", "/"));
            if (!node.isMissingNode() && !node.isNull()) {
                return node.asBoolean();
            }
        }

        // Fallback: check status field
        String status = extractPaymentStatus(eventNode);
        return "COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    /**
     * Extract payment status from event
     */
    private String extractPaymentStatus(JsonNode eventNode) {
        String[] statusPaths = {
                "status", "Status", "paymentStatus", "payment_status",
                "data.status", "data.Status", "data.paymentStatus", "data.payment_status",
                "Data.status", "Data.Status", "Data.paymentStatus", "Data.payment_status"
        };

        for (String path : statusPaths) {
            JsonNode node = eventNode.at("/" + path.replace(".", "/"));
            if (!node.isMissingNode() && !node.isNull()) {
                return node.asText();
            }
        }

        return null;
    }

    /**
     * Find any field that contains specific keywords
     */
    private String findFieldContaining(JsonNode node, String... keywords) {
        if (node.isObject()) {
            var fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                boolean containsAll = true;
                for (String keyword : keywords) {
                    if (!fieldName.toLowerCase().contains(keyword.toLowerCase())) {
                        containsAll = false;
                        break;
                    }
                }
                if (containsAll) {
                    JsonNode fieldValue = node.get(fieldName);
                    if (!fieldValue.isMissingNode() && !fieldValue.isNull()) {
                        return fieldValue.asText();
                    }
                }
            }
        }

        return null;
    }

    // Keep existing methods for backward compatibility
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_SHIPPING_UPDATE, groupId = "${spring.kafka.consumer.group-id}")
    public void listenShippingUpdate(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(eventNode.path("orderId").asText());
            String status = eventNode.path("status").asText();

            // Map shipping status to order status
            OrderStatus orderStatus;
            switch (status.toUpperCase()) {
                case "SHIPPED":
                    orderStatus = OrderStatus.SHIPPED;
                    break;
                case "DELIVERED":
                    orderStatus = OrderStatus.DELIVERED;
                    break;
                case "PROCESSING":
                    orderStatus = OrderStatus.PROCESSING;
                    break;
                default:
                    log.warn("Unknown shipping status: {}", status);
                    return;
            }

            // Update order status
            orderService.updateOrderStatus(orderId, orderStatus);
            log.info("Updated order status to {} after shipping update. Order ID: {}", orderStatus, orderId);

        } catch (EntityNotFoundException e) {
            log.error("Order not found for shipping update event", e);
        } catch (Exception e) {
            log.error("Error processing shipping update event", e);
        }
    }
}