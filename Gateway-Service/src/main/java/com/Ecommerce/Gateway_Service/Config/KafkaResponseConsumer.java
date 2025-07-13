package com.Ecommerce.Gateway_Service.Config;

import com.Ecommerce.Gateway_Service.DTOs.EnrichedCartItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.EnrichedShoppingCartResponse;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.ProductBatchResponseDTO;
import com.Ecommerce.Gateway_Service.DTOs.ProductBatchInfoDTO;
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
public class KafkaResponseConsumer {

    private final AsyncResponseManager asyncResponseManager;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Listen for cart responses from cart service
     */
    @KafkaListener(
            topics = "cart.response",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCartResponse(ConsumerRecord<String, Object> record) {
        Object responsePayload = record.value();
        log.info("Received cart response: {}", responsePayload);

        try {
            Map<String, Object> responseMap = convertToMap(responsePayload);
            String correlationId = (String) responseMap.get("correlationId");
            Boolean success = (Boolean) responseMap.get("success");
            String message = (String) responseMap.get("message");

            log.info("Processing cart response - correlationId: {}, success: {}, message: {}",
                    correlationId, success, message);

            if (correlationId == null) {
                log.error("No correlationId in response: {}", responsePayload);
                return;
            }

            if (success != null && success) {
                Map<String, Object> cartData = (Map<String, Object>) responseMap.get("data");

                if (cartData != null) {
                    EnrichedShoppingCartResponse.EnrichedCartResponseDTO enrichedResponse =
                            convertToEnrichedResponse(cartData);

                    log.info("Completing cart request for correlationId: {} with {} items",
                            correlationId, enrichedResponse.getItemCount());

                    asyncResponseManager.completeRequest(correlationId, enrichedResponse);
                } else {
                    log.error("No cart data in successful response: {}", responsePayload);
                    asyncResponseManager.completeRequestExceptionally(correlationId,
                            new RuntimeException("No cart data in response"));
                }
            } else {
                log.error("Cart response indicates failure: {}", message);
                asyncResponseManager.completeRequestExceptionally(correlationId,
                        new RuntimeException("Cart service error: " + message));
            }

        } catch (Exception e) {
            log.error("Error processing cart response: {}", responsePayload, e);
            handleResponseError(responsePayload, e);
        }
    }

    /**
     * ✅ Listen for product batch responses from product service
     */
    @KafkaListener(
            topics = "product.batch.response",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductBatchResponse(ConsumerRecord<String, Object> record) {
        Object responsePayload = record.value();
        log.info("Received product batch response: {}", responsePayload);

        try {
            Map<String, Object> responseMap = convertToMap(responsePayload);
            String correlationId = (String) responseMap.get("correlationId");
            Boolean success = (Boolean) responseMap.get("success");
            String message = (String) responseMap.get("message");

            log.info("Processing product batch response - correlationId: {}, success: {}, message: {}",
                    correlationId, success, message);

            if (correlationId == null) {
                log.error("No correlationId in product response: {}", responsePayload);
                return;
            }

            if (success != null && success) {
                // Convert to ProductBatchResponseDTO and complete the request
                ProductBatchResponseDTO productResponse = convertToProductBatchResponse(responseMap);

                log.info("Completing product request for correlationId: {} with {} products",
                        correlationId, productResponse.getProducts().size());

                asyncResponseManager.completeRequest(correlationId, productResponse);
            } else {
                log.error("Product response indicates failure: {}", message);
                asyncResponseManager.completeRequestExceptionally(correlationId,
                        new RuntimeException("Product service error: " + message));
            }

        } catch (Exception e) {
            log.error("Error processing product batch response: {}", responsePayload, e);
            handleResponseError(responsePayload, e);
        }
    }

    /**
     * ✅ Listen for cart error responses
     */
    @KafkaListener(
            topics = "cart.error",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCartError(ConsumerRecord<String, Object> record) {
        Object errorPayload = record.value();
        log.error("Received cart error response: {}", errorPayload);
        handleErrorResponse(errorPayload, "Cart service error");
    }

    /**
     * ✅ Listen for product error responses
     */
    @KafkaListener(
            topics = "product.error",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductError(ConsumerRecord<String, Object> record) {
        Object errorPayload = record.value();
        log.error("Received product error response: {}", errorPayload);
        handleErrorResponse(errorPayload, "Product service error");
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
     * ✅ Convert cart service response to enriched response format - FIXED with better error handling
     */
    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO convertToEnrichedResponse(Map<String, Object> cartData) {
        try {
            log.info("Converting cart data: {}", cartData);

            // Extract basic cart info with proper null checks and detailed logging
            String cartIdStr = extractStringValue(cartData, "id");
            String userIdStr = extractStringValue(cartData, "userId");
            Number totalNum = (Number) cartData.get("total");

            log.info("Extracted cart fields - cartId: '{}', userId: '{}', total: {}",
                    cartIdStr, userIdStr, totalNum);

            // Parse UUIDs with better error handling
            UUID cartId = parseUUID(cartIdStr, "cartId");
            UUID userId = parseUUID(userIdStr, "userId");

            // Convert total with validation
            BigDecimal total = convertToBigDecimal(totalNum, "total");

            log.info("Parsed UUIDs - cartId: {}, userId: {}, total: {}", cartId, userId, total);

            // Extract and convert items
            List<EnrichedCartItemDTO> enrichedItems = new ArrayList<>();
            Object itemsObj = cartData.get("items");
            int itemCount = 0;
            int totalQuantity = 0;

            if (itemsObj instanceof List) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                itemCount = items.size();

                log.info("Cart contains {} items", itemCount);

                if (!items.isEmpty()) {
                    for (Map<String, Object> item : items) {
                        try {
                            EnrichedCartItemDTO enrichedItem = convertCartItemToEnrichedItem(item);
                            if (enrichedItem != null) {
                                enrichedItems.add(enrichedItem);
                                totalQuantity += (enrichedItem.getQuantity() != null ? enrichedItem.getQuantity() : 0);
                            }
                        } catch (Exception e) {
                            log.error("Error converting cart item: {}", item, e);
                            // Continue processing other items
                        }
                    }

                    log.info("Successfully processed {} cart items with total quantity: {}",
                            enrichedItems.size(), totalQuantity);
                } else {
                    log.info("Cart is empty - no items to process");
                }
            } else {
                log.warn("Items object is not a List, type: {}, value: {}",
                        itemsObj != null ? itemsObj.getClass().getSimpleName() : "null", itemsObj);
            }

            // Parse datetime fields with detailed logging
            LocalDateTime createdAt = parseDateTime(cartData.get("createdAt"));
            LocalDateTime updatedAt = parseDateTime(cartData.get("updatedAt"));
            LocalDateTime expiresAt = parseDateTime(cartData.get("expiresAt"));

            log.info("Parsed timestamps - createdAt: {}, updatedAt: {}, expiresAt: {}",
                    createdAt, updatedAt, expiresAt);

            // Build the response
            EnrichedShoppingCartResponse.EnrichedCartResponseDTO result =
                    EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                            .id(cartId)
                            .userId(userId)
                            .items(enrichedItems)
                            .total(total)
                            .itemCount(itemCount)
                            .totalQuantity(totalQuantity)
                            .createdAt(createdAt)
                            .updatedAt(updatedAt)
                            .expiresAt(expiresAt)
                            .build();

            log.info("✅ FINAL CART RESPONSE: id={}, userId={}, itemCount={}, totalQuantity={}, total={}, createdAt={}, updatedAt={}, expiresAt={}",
                    result.getId(), result.getUserId(), result.getItemCount(),
                    result.getTotalQuantity(), result.getTotal(), result.getCreatedAt(),
                    result.getUpdatedAt(), result.getExpiresAt());

            return result;

        } catch (Exception e) {
            log.error("Error converting cart data to enriched response: {}", cartData, e);
            return createFallbackResponse(cartData);
        }
    }

    /**
     * ✅ Convert cart item to EnrichedCartItemDTO - FIXED with better error handling
     */
    private EnrichedCartItemDTO convertCartItemToEnrichedItem(Map<String, Object> item) {
        try {
            log.debug("Converting cart item: {}", item);

            // Extract item fields with detailed logging
            String itemIdStr = extractStringValue(item, "id");
            String productIdStr = extractStringValue(item, "productId");
            Number quantityNum = (Number) item.get("quantity");
            Number priceNum = (Number) item.get("price");
            Number subtotalNum = (Number) item.get("subtotal");

            log.debug("Item field extraction - id: '{}', productId: '{}', quantity: {}, price: {}, subtotal: {}",
                    itemIdStr, productIdStr, quantityNum, priceNum, subtotalNum);

            // Parse UUIDs
            UUID itemId = parseUUID(itemIdStr, "itemId");
            UUID productId = parseUUID(productIdStr, "productId");

            // Convert numeric fields
            Integer quantity = convertToInteger(quantityNum, "quantity");
            BigDecimal price = convertToBigDecimal(priceNum, "price");
            BigDecimal subtotal = convertToBigDecimal(subtotalNum, "subtotal");

            // Calculate subtotal if not provided
            if ((subtotal == null || subtotal.equals(BigDecimal.ZERO)) && price != null && quantity != null) {
                subtotal = price.multiply(BigDecimal.valueOf(quantity));
                log.debug("Calculated subtotal: {} (price: {} * quantity: {})", subtotal, price, quantity);
            }

            // Parse datetime
            LocalDateTime addedAt = parseDateTime(item.get("addedAt"));

            EnrichedCartItemDTO enrichedItem = EnrichedCartItemDTO.builder()
                    .id(itemId)
                    .productId(productId)
                    .quantity(quantity)
                    .price(price)
                    .subtotal(subtotal)
                    .addedAt(addedAt)
                    .productName("Loading...") // Will be enriched by product service
                    .productImage(null)
                    .productStatus(null)
                    .availableQuantity(0)
                    .inStock(null)
                    .build();

            log.debug("✅ Converted cart item: id={}, productId={}, quantity={}, price={}, subtotal={}, addedAt={}",
                    enrichedItem.getId(), enrichedItem.getProductId(), enrichedItem.getQuantity(),
                    enrichedItem.getPrice(), enrichedItem.getSubtotal(), enrichedItem.getAddedAt());

            return enrichedItem;

        } catch (Exception e) {
            log.error("Error converting cart item: {}", item, e);
            return createFallbackErrorItem();
        }
    }

    /**
     * ✅ Enhanced helper methods for type conversion with better error handling
     */
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

    /**
     * ✅ Enhanced datetime parsing with better error handling
     */
    private LocalDateTime parseDateTime(Object dateTimeObj) {
        if (dateTimeObj == null) {
            log.debug("DateTime object is null");
            return null;
        }

        try {
            if (dateTimeObj instanceof List) {
                // Handle array format like [2025, 7, 11, 23, 37, 35, 974000000]
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
                } else {
                    log.warn("DateTime array has insufficient elements: {}", dateArray);
                }
            } else if (dateTimeObj instanceof String) {
                LocalDateTime result = LocalDateTime.parse((String) dateTimeObj);
                log.debug("Parsed datetime from string: {} -> {}", dateTimeObj, result);
                return result;
            } else {
                log.warn("Unexpected datetime format: {} ({})", dateTimeObj, dateTimeObj.getClass().getSimpleName());
            }

        } catch (Exception e) {
            log.error("Error parsing datetime: {} - {}", dateTimeObj, e.getMessage());
        }

        return null; // Return null instead of current time for debugging
    }

    private EnrichedCartItemDTO createFallbackErrorItem() {
        return EnrichedCartItemDTO.builder()
                .id(null)
                .productId(null)
                .quantity(0)
                .price(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .addedAt(null)
                .productName("Error loading item")
                .productImage(null)
                .productStatus(null)
                .availableQuantity(0)
                .inStock(false)
                .build();
    }

    /**
     * ✅ Convert product service response to ProductBatchResponseDTO
     */
    private ProductBatchResponseDTO convertToProductBatchResponse(Map<String, Object> responseMap) {
        try {
            List<ProductBatchInfoDTO> products = Collections.emptyList();

            // Extract products list if present
            Object productsObj = responseMap.get("products");
            if (productsObj instanceof List) {
                List<Map<String, Object>> productMaps = (List<Map<String, Object>>) productsObj;
                products = productMaps.stream()
                        .map(this::convertToProductBatchInfoDTO)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            return ProductBatchResponseDTO.builder()
                    .correlationId((String) responseMap.get("correlationId"))
                    .success((Boolean) responseMap.get("success"))
                    .message((String) responseMap.get("message"))
                    .products(products)
                    .timestamp(getLongValue(responseMap.get("timestamp")))
                    .build();
        } catch (Exception e) {
            log.error("Error converting product batch response: {}", responseMap, e);
            return ProductBatchResponseDTO.builder()
                    .correlationId((String) responseMap.get("correlationId"))
                    .success(false)
                    .message("Error parsing response: " + e.getMessage())
                    .products(Collections.emptyList())
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    /**
     * ✅ Convert map to ProductBatchInfoDTO
     */
    private ProductBatchInfoDTO convertToProductBatchInfoDTO(Map<String, Object> productMap) {
        try {
            String productIdStr = extractStringValue(productMap, "id");
            UUID productId = parseUUID(productIdStr, "productId");

            return ProductBatchInfoDTO.builder()
                    .id(productId)
                    .name((String) productMap.get("name"))
                    .imagePath((String) productMap.get("imagePath"))
                    .inStock((Boolean) productMap.get("inStock"))
                    .availableQuantity(convertToInteger((Number) productMap.get("availableQuantity"), "availableQuantity"))
                    .status((String) productMap.get("status"))
                    .price(convertToBigDecimal((Number) productMap.get("price"), "price"))
                    .build();
        } catch (Exception e) {
            log.error("Error converting product info: {}", productMap, e);
            return null;
        }
    }

    /**
     * ✅ Helper methods for type conversion
     */
    private Long getLongValue(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return System.currentTimeMillis();
    }

    /**
     * ✅ Handle error responses
     */
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

    /**
     * ✅ Handle response processing errors
     */
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

    /**
     * ✅ Extract correlationId from payload
     */
    private String extractCorrelationId(Object payload) throws Exception {
        Map<String, Object> map = convertToMap(payload);
        return (String) map.get("correlationId");
    }

    /**
     * ✅ Create fallback response with more realistic defaults
     */
    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO createFallbackResponse(Map<String, Object> cartData) {
        String userIdStr = extractStringValue(cartData, "userId");
        UUID userId = parseUUID(userIdStr, "fallback_userId");

        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .id(null)
                .userId(userId)
                .items(List.of())
                .total(BigDecimal.ZERO)
                .itemCount(0)
                .totalQuantity(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();
    }
}