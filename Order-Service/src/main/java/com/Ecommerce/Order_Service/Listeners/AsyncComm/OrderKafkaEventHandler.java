package com.Ecommerce.Order_Service.Listeners.AsyncComm;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaEventHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Listen for order requests from Gateway Service
     */
    @KafkaListener(
            topics = "order.request",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(readOnly = true)
    public void handleOrderRequest(ConsumerRecord<String, Object> record) {
        Object requestPayload = record.value();
        String correlationId = record.key();

        log.info("📦 ORDER SERVICE: Received order request with correlationId: {}", correlationId);
        log.info("📦 ORDER SERVICE: Request payload: {}", requestPayload);

        try {
            // Parse the request
            Map<String, Object> request = convertToMap(requestPayload);
            String orderId = (String) request.get("orderId");

            if (orderId == null || orderId.trim().isEmpty()) {
                sendErrorResponse(correlationId, "Order ID is required");
                return;
            }

            log.info("📦 ORDER SERVICE: Fetching order with ID: {}", orderId);

            // Fetch order from database
            Optional<Order> orderOptional = orderRepository.findById(UUID.fromString(orderId));

            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                log.info("📦 ORDER SERVICE: Found order - ID: {}, Status: {}, Items: {}",
                        order.getId(), order.getStatus(), order.getItems().size());

                // Convert to response format
                Map<String, Object> orderData = convertOrderToMap(order);

                // Send successful response
                sendSuccessResponse(correlationId, orderData);

            } else {
                log.warn("📦 ORDER SERVICE: Order not found for ID: {}", orderId);
                sendErrorResponse(correlationId, "Order not found with ID: " + orderId);
            }

        } catch (IllegalArgumentException e) {
            log.error("📦 ORDER SERVICE: Invalid UUID format", e);
            sendErrorResponse(correlationId, "Invalid order ID format: " + e.getMessage());
        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error processing order request", e);
            sendErrorResponse(correlationId, "Internal error: " + e.getMessage());
        }
    }

    /**
     * ✅ Convert Order entity to Map format expected by Gateway
     */
    private Map<String, Object> convertOrderToMap(Order order) {
        Map<String, Object> orderMap = new HashMap<>();

        // Basic order fields
        orderMap.put("id", order.getId().toString());
        orderMap.put("userId", order.getUserId().toString());
        orderMap.put("cartId", order.getCartId() != null ? order.getCartId().toString() : null);
        orderMap.put("status", order.getStatus().toString());
        orderMap.put("totalAmount", order.getTotalAmount());
        orderMap.put("tax", order.getTax());
        orderMap.put("shippingCost", order.getShippingCost());
        orderMap.put("discount", order.getDiscount());
        orderMap.put("createdAt", order.getCreatedAt());
        orderMap.put("updatedAt", order.getUpdatedAt());
        orderMap.put("billingAddressId", order.getBillingAddressId() != null ?
                order.getBillingAddressId().toString() : null);
        orderMap.put("shippingAddressId", order.getShippingAddressId() != null ?
                order.getShippingAddressId().toString() : null);

        // Convert order items
        List<Map<String, Object>> items = order.getItems().stream()
                .map(this::convertOrderItemToMap)
                .collect(Collectors.toList());

        orderMap.put("items", items);

        log.info("📦 ORDER SERVICE: Converted order to map - ID: {}, Items: {}",
                order.getId(), items.size());

        return orderMap;
    }

    /**
     * ✅ Convert OrderItem entity to Map
     */
    private Map<String, Object> convertOrderItemToMap(OrderItem item) {
        Map<String, Object> itemMap = new HashMap<>();

        itemMap.put("id", item.getId().toString());
        itemMap.put("productId", item.getProductId().toString());
        itemMap.put("quantity", item.getQuantity());
        itemMap.put("priceAtPurchase", item.getPriceAtPurchase());
        itemMap.put("discount", item.getDiscount());
        itemMap.put("total", item.getTotal());

        return itemMap;
    }

    /**
     * ✅ Send successful response to Gateway
     */
    private void sendSuccessResponse(String correlationId, Map<String, Object> orderData) {
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("success", true);
        response.put("message", "Order retrieved successfully");
        response.put("data", orderData);
        response.put("timestamp", System.currentTimeMillis());

        log.info("📦 ORDER SERVICE: Sending success response for correlationId: {}", correlationId);

        kafkaTemplate.send("order.response", correlationId, response)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("📦 ORDER SERVICE: Failed to send success response", ex);
                    } else {
                        log.info("📦 ORDER SERVICE: Successfully sent response to order.response topic");
                    }
                });
    }

    /**
     * ✅ Send error response to Gateway
     */
    private void sendErrorResponse(String correlationId, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("success", false);
        response.put("message", errorMessage);
        response.put("timestamp", System.currentTimeMillis());

        log.error("📦 ORDER SERVICE: Sending error response for correlationId: {} - {}",
                correlationId, errorMessage);

        kafkaTemplate.send("order.error", correlationId, response)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("📦 ORDER SERVICE: Failed to send error response", ex);
                    } else {
                        log.info("📦 ORDER SERVICE: Successfully sent error to order.error topic");
                    }
                });
    }

    /**
     * ✅ Convert various payload types to Map
     */
    private Map<String, Object> convertToMap(Object payload) throws Exception {
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        } else if (payload instanceof String) {
            return objectMapper.readValue((String) payload, Map.class);
        } else {
            String jsonString = objectMapper.writeValueAsString(payload);
            return objectMapper.readValue(jsonString, Map.class);
        }
    }

    /**
     * ✅ Handle batch order requests (optional - for future use)
     */
    @KafkaListener(
            topics = "order.batch.request",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(readOnly = true)
    public void handleOrderBatchRequest(ConsumerRecord<String, Object> record) {
        Object requestPayload = record.value();
        String correlationId = record.key();

        log.info("📦 ORDER SERVICE: Received batch order request with correlationId: {}", correlationId);

        try {
            Map<String, Object> request = convertToMap(requestPayload);
            String userId = (String) request.get("userId");
            String status = (String) request.get("status");
            Integer limit = (Integer) request.get("limit");

            if (userId == null || userId.trim().isEmpty()) {
                sendBatchErrorResponse(correlationId, "User ID is required for batch request");
                return;
            }

            log.info("📦 ORDER SERVICE: Fetching orders for user: {} with status: {}", userId, status);

            // Fetch orders based on criteria
            List<Order> orders;
            if (status != null && !status.isEmpty()) {
                orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        UUID.fromString(userId),
                        OrderStatus.valueOf(status)
                );
            } else {
                orders = orderRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId));
            }

            // Apply limit if specified
            if (limit != null && limit > 0) {
                orders = orders.stream().limit(limit).collect(Collectors.toList());
            }

            log.info("📦 ORDER SERVICE: Found {} orders for user: {}", orders.size(), userId);

            // Convert orders to response format
            List<Map<String, Object>> orderDataList = orders.stream()
                    .map(this::convertOrderToMap)
                    .collect(Collectors.toList());

            // Send successful response
            sendBatchSuccessResponse(correlationId, orderDataList);

        } catch (Exception e) {
            log.error("📦 ORDER SERVICE: Error processing batch order request", e);
            sendBatchErrorResponse(correlationId, "Internal error: " + e.getMessage());
        }
    }

    /**
     * ✅ Send batch success response
     */
    private void sendBatchSuccessResponse(String correlationId, List<Map<String, Object>> orders) {
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("success", true);
        response.put("message", "Orders retrieved successfully");
        response.put("data", orders);
        response.put("count", orders.size());
        response.put("timestamp", System.currentTimeMillis());

        log.info("📦 ORDER SERVICE: Sending batch success response with {} orders", orders.size());

        kafkaTemplate.send("order.batch.response", correlationId, response);
    }

    /**
     * ✅ Send batch error response
     */
    private void sendBatchErrorResponse(String correlationId, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("success", false);
        response.put("message", errorMessage);
        response.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send("order.batch.error", correlationId, response);
    }
}