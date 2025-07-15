package com.Ecommerce.Gateway_Service.Consumer;


import com.Ecommerce.Gateway_Service.DTOs.EnrichedOrderItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.Order.EnrichedOrderResponse;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderResponseConsumer {

    private final AsyncResponseManager asyncResponseManager;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Listen for order responses from order service
     */
    @KafkaListener(
            topics = "order.response",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderResponse(ConsumerRecord<String, Object> record) {
        Object responsePayload = record.value();
        log.info("Received order response: {}", responsePayload);

        try {
            Map<String, Object> responseMap = convertToMap(responsePayload);
            String correlationId = (String) responseMap.get("correlationId");
            Boolean success = (Boolean) responseMap.get("success");
            String message = (String) responseMap.get("message");

            log.info("Processing order response - correlationId: {}, success: {}, message: {}",
                    correlationId, success, message);

            if (correlationId == null) {
                log.error("No correlationId in order response: {}", responsePayload);
                return;
            }

            if (success != null && success) {
                Map<String, Object> orderData = (Map<String, Object>) responseMap.get("data");

                if (orderData != null) {
                    EnrichedOrderResponse enrichedResponse = convertToEnrichedOrderResponse(orderData);

                    log.info("Completing order request for correlationId: {} with {} items",
                            correlationId, enrichedResponse.getItemCount());

                    asyncResponseManager.completeRequest(correlationId, enrichedResponse);
                } else {
                    log.error("No order data in successful response: {}", responsePayload);
                    asyncResponseManager.completeRequestExceptionally(correlationId,
                            new RuntimeException("No order data in response"));
                }
            } else {
                log.error("Order response indicates failure: {}", message);
                asyncResponseManager.completeRequestExceptionally(correlationId,
                        new RuntimeException("Order service error: " + message));
            }

        } catch (Exception e) {
            log.error("Error processing order response: {}", responsePayload, e);
            handleResponseError(responsePayload, e);
        }
    }

    /**
     * ✅ Listen for order error responses
     */
    @KafkaListener(
            topics = "order.error",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderError(ConsumerRecord<String, Object> record) {
        Object errorPayload = record.value();
        log.error("Received order error response: {}", errorPayload);
        handleErrorResponse(errorPayload, "Order service error");
    }

    /**
     * ✅ Convert order service response to enriched order response format
     */
    private EnrichedOrderResponse convertToEnrichedOrderResponse(Map<String, Object> orderData) {
        try {
            log.info("Converting order data: {}", orderData);

            // Extract basic order info
            String orderIdStr = extractStringValue(orderData, "id");
            String userIdStr = extractStringValue(orderData, "userId");
            String cartIdStr = extractStringValue(orderData, "cartId");
            String billingAddressIdStr = extractStringValue(orderData, "billingAddressId");
            String shippingAddressIdStr = extractStringValue(orderData, "shippingAddressId");

            // Parse UUIDs
            UUID orderId = parseUUID(orderIdStr, "orderId");
            UUID userId = parseUUID(userIdStr, "userId");
            UUID cartId = parseUUID(cartIdStr, "cartId");
            UUID billingAddressId = parseUUID(billingAddressIdStr, "billingAddressId");
            UUID shippingAddressId = parseUUID(shippingAddressIdStr, "shippingAddressId");

            // Convert numeric fields
            BigDecimal totalAmount = convertToBigDecimal((Number) orderData.get("totalAmount"), "totalAmount");
            BigDecimal tax = convertToBigDecimal((Number) orderData.get("tax"), "tax");
            BigDecimal shippingCost = convertToBigDecimal((Number) orderData.get("shippingCost"), "shippingCost");
            BigDecimal discount = convertToBigDecimal((Number) orderData.get("discount"), "discount");

            // Extract status
            String status = (String) orderData.get("status");

            // Extract and convert items
            List<EnrichedOrderItemDTO> enrichedItems = new ArrayList<>();
            Object itemsObj = orderData.get("items");

            if (itemsObj instanceof List) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                log.info("Order contains {} items", items.size());

                for (Map<String, Object> item : items) {
                    try {
                        EnrichedOrderItemDTO enrichedItem = convertOrderItemToEnrichedItem(item);
                        if (enrichedItem != null) {
                            enrichedItems.add(enrichedItem);
                        }
                    } catch (Exception e) {
                        log.error("Error converting order item: {}", item, e);
                    }
                }

                log.info("Successfully processed {} order items", enrichedItems.size());
            }

            // Parse datetime fields
            LocalDateTime createdAt = parseDateTime(orderData.get("createdAt"));
            LocalDateTime updatedAt = parseDateTime(orderData.get("updatedAt"));

            // Build the response
            EnrichedOrderResponse result = EnrichedOrderResponse.builder()
                    .id(orderId)
                    .userId(userId)
                    .cartId(cartId)
                    .status(status)
                    .items(enrichedItems)
                    .totalAmount(totalAmount)
                    .tax(tax)
                    .shippingCost(shippingCost)
                    .discount(discount)
                    .billingAddressId(billingAddressId)
                    .shippingAddressId(shippingAddressId)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();

            log.info("✅ FINAL ORDER RESPONSE: id={}, userId={}, status={}, itemCount={}, totalAmount={}",
                    result.getId(), result.getUserId(), result.getStatus(),
                    result.getItemCount(), result.getTotalAmount());

            return result;

        } catch (Exception e) {
            log.error("Error converting order data to enriched response: {}", orderData, e);
            return createFallbackOrderResponse(orderData);
        }
    }

    /**
     * ✅ Convert order item to EnrichedOrderItemDTO
     */
    private EnrichedOrderItemDTO convertOrderItemToEnrichedItem(Map<String, Object> item) {
        try {
            log.debug("Converting order item: {}", item);

            // Extract item fields
            String itemIdStr = extractStringValue(item, "id");
            String productIdStr = extractStringValue(item, "productId");
            Number quantityNum = (Number) item.get("quantity");
            Number priceNum = (Number) item.get("priceAtPurchase");
            Number discountNum = (Number) item.get("discount");
            Number totalNum = (Number) item.get("total");

            // Parse UUIDs
            UUID itemId = parseUUID(itemIdStr, "itemId");
            UUID productId = parseUUID(productIdStr, "productId");

            // Convert numeric fields
            Integer quantity = convertToInteger(quantityNum, "quantity");
            BigDecimal priceAtPurchase = convertToBigDecimal(priceNum, "priceAtPurchase");
            BigDecimal discount = convertToBigDecimal(discountNum, "discount");
            BigDecimal total = convertToBigDecimal(totalNum, "total");

            EnrichedOrderItemDTO enrichedItem = EnrichedOrderItemDTO.builder()
                    .id(itemId)
                    .productId(productId)
                    .quantity(quantity)
                    .priceAtPurchase(priceAtPurchase)
                    .discount(discount)
                    .total(total)
                    .productName("Loading...") // Will be enriched by product service
                    .productImage(null)
                    .productStatus(null)
                    .availableQuantity(0)
                    .inStock(null)
                    .build();

            log.debug("✅ Converted order item: id={}, productId={}, quantity={}, priceAtPurchase={}, total={}",
                    enrichedItem.getId(), enrichedItem.getProductId(), enrichedItem.getQuantity(),
                    enrichedItem.getPriceAtPurchase(), enrichedItem.getTotal());

            return enrichedItem;

        } catch (Exception e) {
            log.error("Error converting order item: {}", item, e);
            return createFallbackOrderItem();
        }
    }

    /**
     * ✅ Helper methods - reusing from the existing KafkaResponseConsumer
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

    private String extractStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            log.warn("Missing field '{}' in map", key);
            return null;
        }
        String stringValue = value.toString().trim();
        if (stringValue.isEmpty()) {
            log.warn("Empty field '{}' in map", key);
            return null;
        }
        return stringValue;
    }

    private UUID parseUUID(String uuidStr, String fieldName) {
        if (uuidStr == null || uuidStr.trim().isEmpty()) {
            log.warn("Cannot parse UUID for field '{}': value is null or empty", fieldName);
            return null;
        }

        try {
            UUID result = UUID.fromString(uuidStr.trim());
            log.debug("Successfully parsed UUID for field '{}': {}", fieldName, result);
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for field '{}': '{}' - {}", fieldName, uuidStr, e.getMessage());
            return null;
        }
    }

    private BigDecimal convertToBigDecimal(Number value, String fieldName) {
        if (value == null) {
            log.warn("BigDecimal field '{}' is null, defaulting to ZERO", fieldName);
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal result = BigDecimal.valueOf(value.doubleValue());
            log.debug("Converted {} for field '{}': {}", value.getClass().getSimpleName(), fieldName, result);
            return result;
        } catch (Exception e) {
            log.error("Error converting {} to BigDecimal for field '{}': {} - {}",
                    value.getClass().getSimpleName(), fieldName, value, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Integer convertToInteger(Number value, String fieldName) {
        if (value == null) {
            log.warn("Integer field '{}' is null, defaulting to 0", fieldName);
            return 0;
        }

        try {
            Integer result = value.intValue();
            log.debug("Converted {} for field '{}': {}", value.getClass().getSimpleName(), fieldName, result);
            return result;
        } catch (Exception e) {
            log.error("Error converting {} to Integer for field '{}': {} - {}",
                    value.getClass().getSimpleName(), fieldName, value, e.getMessage());
            return 0;
        }
    }

    private LocalDateTime parseDateTime(Object dateTimeObj) {
        if (dateTimeObj == null) {
            log.debug("DateTime object is null");
            return null;
        }

        try {
            if (dateTimeObj instanceof List) {
                List<Number> dateArray = (List<Number>) dateTimeObj;
                if (dateArray.size() >= 6) {
                    LocalDateTime result = LocalDateTime.of(
                            dateArray.get(0).intValue(), // year
                            dateArray.get(1).intValue(), // month
                            dateArray.get(2).intValue(), // day
                            dateArray.get(3).intValue(), // hour
                            dateArray.get(4).intValue(), // minute
                            dateArray.get(5).intValue(), // second
                            dateArray.size() > 6 ? dateArray.get(6).intValue() : 0 // nano
                    );
                    log.debug("Parsed datetime from array: {} -> {}", dateArray, result);
                    return result;
                }
            } else if (dateTimeObj instanceof String) {
                LocalDateTime result = LocalDateTime.parse((String) dateTimeObj);
                log.debug("Parsed datetime from string: {} -> {}", dateTimeObj, result);
                return result;
            }
        } catch (Exception e) {
            log.error("Error parsing datetime: {} - {}", dateTimeObj, e.getMessage());
        }

        return null;
    }

    private EnrichedOrderItemDTO createFallbackOrderItem() {
        return EnrichedOrderItemDTO.builder()
                .id(null)
                .productId(null)
                .quantity(0)
                .priceAtPurchase(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .productName("Error loading item")
                .productImage(null)
                .productStatus(null)
                .availableQuantity(0)
                .inStock(false)
                .build();
    }

    private void handleErrorResponse(Object errorPayload, String errorType) {
        try {
            Map<String, Object> errorMap = convertToMap(errorPayload);
            String correlationId = (String) errorMap.get("correlationId");
            String message = (String) errorMap.get("message");

            if (correlationId != null) {
                asyncResponseManager.completeRequestExceptionally(correlationId,
                        new RuntimeException(errorType + ": " + message));
            }
        } catch (Exception e) {
            log.error("Error processing error response: {}", errorPayload, e);
        }
    }

    private void handleResponseError(Object payload, Exception error) {
        try {
            String correlationId = extractCorrelationId(payload);
            if (correlationId != null) {
                asyncResponseManager.completeRequestExceptionally(correlationId, error);
            }
        } catch (Exception extractError) {
            log.error("Failed to extract correlationId from error response", extractError);
        }
    }

    private String extractCorrelationId(Object payload) throws Exception {
        Map<String, Object> map = convertToMap(payload);
        return (String) map.get("correlationId");
    }

    private EnrichedOrderResponse createFallbackOrderResponse(Map<String, Object> orderData) {
        String userIdStr = extractStringValue(orderData, "userId");
        UUID userId = parseUUID(userIdStr, "fallback_userId");

        return EnrichedOrderResponse.builder()
                .id(null)
                .userId(userId)
                .items(List.of())
                .totalAmount(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .shippingCost(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .status("UNKNOWN")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * ✅ Listen for user orders responses from order service
     */
    @KafkaListener(
            topics = "user.orders.response",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserOrdersResponse(ConsumerRecord<String, Object> record) {
        Object responsePayload = record.value();
        log.info("Received user orders response: {}", responsePayload);

        try {
            Map<String, Object> responseMap = convertToMap(responsePayload);
            String correlationId = (String) responseMap.get("correlationId");
            Boolean success = (Boolean) responseMap.get("success");
            String message = (String) responseMap.get("message");

            log.info("Processing user orders response - correlationId: {}, success: {}, message: {}",
                    correlationId, success, message);

            if (correlationId == null) {
                log.error("No correlationId in user orders response: {}", responsePayload);
                return;
            }

            if (success != null && success) {
                Object orderIdsData = responseMap.get("data");

                if (orderIdsData != null) {
                    List<String> orderIds = convertToOrderIdsList(orderIdsData);

                    log.info("Completing user orders request for correlationId: {} with {} order IDs",
                            correlationId, orderIds.size());

                    asyncResponseManager.completeRequest(correlationId, orderIds);
                } else {
                    log.error("No order IDs data in successful user orders response: {}", responsePayload);
                    asyncResponseManager.completeRequest(correlationId, List.of()); // Empty list
                }
            } else {
                log.error("User orders response indicates failure: {}", message);
                asyncResponseManager.completeRequestExceptionally(correlationId,
                        new RuntimeException("User orders service error: " + message));
            }

        } catch (Exception e) {
            log.error("Error processing user orders response: {}", responsePayload, e);
            handleResponseError(responsePayload, e);
        }
    }

    /**
     * ✅ Listen for user orders error responses
     */
    @KafkaListener(
            topics = "user.orders.error",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserOrdersError(ConsumerRecord<String, Object> record) {
        Object errorPayload = record.value();
        log.error("Received user orders error response: {}", errorPayload);
        handleErrorResponse(errorPayload, "User orders service error");
    }

    /**
     * ✅ Convert order IDs data to list of strings
     */
    private List<String> convertToOrderIdsList(Object orderIdsData) {
        try {
            if (orderIdsData instanceof List) {
                List<?> orderIdsList = (List<?>) orderIdsData;
                return orderIdsList.stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            } else if (orderIdsData instanceof String) {
                // Handle comma-separated string
                String orderIdsString = (String) orderIdsData;
                if (orderIdsString.trim().isEmpty()) {
                    return List.of();
                }
                return Arrays.stream(orderIdsString.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .collect(Collectors.toList());
            } else {
                log.warn("Unexpected order IDs data type: {}", orderIdsData.getClass());
                return List.of();
            }
        } catch (Exception e) {
            log.error("Error converting order IDs data: {}", orderIdsData, e);
            return List.of();
        }
    }
}