// Fixed Order-Service/src/main/java/com/Ecommerce/Order_Service/Services/Kafka/EnhancedKafkaConsumerService.java

package com.Ecommerce.Order_Service.Listeners.AsyncComm;

import com.Ecommerce.Order_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Fixed Enhanced service for consuming payment events from Kafka topics
 * and processing them for the Order Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedKafkaConsumerService {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * FIXED: Payment confirmed listener - now accepts Object/Map instead of String
     */
    @KafkaListener(topics = "payment-confirmed", groupId = "order-service-group")
    public void listenPaymentConfirmed(Map<String, Object> paymentEvent) {
        try {
            log.info("💳 ORDER SERVICE: Received payment confirmed event: {}", paymentEvent);

            // Extract order ID from the payment event
            String orderIdStr = extractOrderIdFromEvent(paymentEvent);
            if (orderIdStr == null || orderIdStr.isEmpty()) {
                log.error("💳 ORDER SERVICE: No order ID found in payment confirmed event: {}", paymentEvent);
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr);
            log.info("💳 ORDER SERVICE: Processing payment confirmation for order: {}", orderId);

            // Check if payment was successful
            boolean paymentSuccess = extractPaymentSuccess(paymentEvent);
            String paymentStatus = extractPaymentStatus(paymentEvent);

            log.info("💳 ORDER SERVICE: Payment details - Success: {}, Status: {}", paymentSuccess, paymentStatus);

            if (paymentSuccess || "COMPLETED".equalsIgnoreCase(paymentStatus)) {
                // Update order status to PAID
                Order updatedOrder = orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                log.info("💳 ORDER SERVICE: ✅ Updated order {} status to PAID after payment confirmation", orderId);

                // Log additional payment details
                String paymentId = getStringValue(paymentEvent, "paymentId");
                Double amount = getDoubleValue(paymentEvent, "amount");
                String paymentMethod = getStringValue(paymentEvent, "paymentMethod");

                log.info("💳 ORDER SERVICE: Payment details - ID: {}, Amount: {}, Method: {}",
                        paymentId, amount, paymentMethod);

            } else {
                // Payment failed, update order status accordingly
                orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_FAILED);
                log.warn("💳 ORDER SERVICE: ❌ Updated order {} status to PAYMENT_FAILED", orderId);
            }

        } catch (EntityNotFoundException e) {
            log.error("💳 ORDER SERVICE: Order not found for payment confirmation event: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("💳 ORDER SERVICE: Invalid order ID in payment confirmation event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("💳 ORDER SERVICE: Error processing payment confirmation event: {}", e.getMessage(), e);
        }
    }

    /**
     * FIXED: Payment failed listener - now accepts Object/Map instead of String
     */
    @KafkaListener(topics = "payment-failed", groupId = "order-service-group")
    public void listenPaymentFailed(Map<String, Object> paymentEvent) {
        try {
            log.warn("💳 ORDER SERVICE: Received payment failed event: {}", paymentEvent);

            String orderIdStr = extractOrderIdFromEvent(paymentEvent);

            if (orderIdStr != null && !orderIdStr.isEmpty()) {
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
     * Enhanced method to extract order ID from Map-based event
     */
    private String extractOrderIdFromEvent(Map<String, Object> eventMap) {
        // Try multiple possible field names for order ID
        String[] possibleKeys = {
                "orderId", "order_id", "OrderID", "orderid"
        };

        for (String key : possibleKeys) {
            Object value = eventMap.get(key);
            if (value != null && !value.toString().isEmpty()) {
                String orderIdStr = value.toString();
                log.debug("💳 ORDER SERVICE: Found order ID '{}' with key '{}'", orderIdStr, key);
                return orderIdStr;
            }
        }

        log.error("💳 ORDER SERVICE: No order ID found in event keys: {}", eventMap.keySet());
        return null;
    }

    /**
     * Enhanced method to extract payment success status from Map
     */
    private boolean extractPaymentSuccess(Map<String, Object> eventMap) {
        // Try different keys for success status
        String[] successKeys = {
                "success", "Success", "successful", "Successful"
        };

        for (String key : successKeys) {
            Object value = eventMap.get(key);
            if (value != null) {
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                // Handle string representations
                String strValue = value.toString().toLowerCase();
                return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue);
            }
        }

        // Fallback: check status field
        String status = extractPaymentStatus(eventMap);
        return "COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    /**
     * Enhanced method to extract payment status from Map
     */
    private String extractPaymentStatus(Map<String, Object> eventMap) {
        String[] statusKeys = {
                "status", "Status", "paymentStatus", "payment_status"
        };

        for (String key : statusKeys) {
            Object value = eventMap.get(key);
            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    /**
     * Helper method to safely get String value from Map
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * Helper method to safely get Double value from Map
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("💳 ORDER SERVICE: Could not parse '{}' as double for key '{}'", value, key);
            }
        }
        return 0.0;
    }

    /**
     * Keep existing shipping update method - also needs fixing if it has similar issue
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_SHIPPING_UPDATE, groupId = "order-service-group")
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
}