// Gateway-Service/src/main/java/com/Ecommerce/Gateway_Service/Consumer/KafkaSaved4LaterResponseConsumer.java
package com.Ecommerce.Gateway_Service.Consumer;

import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.SavedItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.SavedItemsResponseDTO;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.Ecommerce.Gateway_Service.Kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaSaved4LaterResponseConsumer {

    private final AsyncResponseManager asyncResponseManager;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Listen for saved4later responses from cart service
     */
    @KafkaListener(
            topics = "saved4later.response",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSaved4LaterResponse(ConsumerRecord<String, Object> record) {
        Object responsePayload = record.value();
        log.info("Received saved4later response: {}", responsePayload);

        try {
            Map<String, Object> responseMap = convertToMap(responsePayload);
            String correlationId = (String) responseMap.get("correlationId");
            Boolean success = (Boolean) responseMap.get("success");
            String message = (String) responseMap.get("message");

            log.info("Processing saved4later response - correlationId: {}, success: {}, message: {}",
                    correlationId, success, message);

            if (correlationId == null) {
                log.error("No correlationId in saved4later response: {}", responsePayload);
                return;
            }

            if (success != null && success) {
                Object savedItemsData = responseMap.get("data");

                if (savedItemsData != null) {
                    SavedItemsResponseDTO savedItemsResponse = convertToSavedItemsResponse(savedItemsData);

                    log.info("Completing saved4later request for correlationId: {} with {} items",
                            correlationId, savedItemsResponse.getItemCount());

                    asyncResponseManager.completeRequest(correlationId, savedItemsResponse);
                } else {
                    log.error("No saved4later data in successful response: {}", responsePayload);
                    asyncResponseManager.completeRequestExceptionally(correlationId,
                            new RuntimeException("No saved4later data in response"));
                }
            } else {
                log.error("Saved4later response indicates failure: {}", message);
                asyncResponseManager.completeRequestExceptionally(correlationId,
                        new RuntimeException("Saved4later service error: " + message));
            }

        } catch (Exception e) {
            log.error("Error processing saved4later response: {}", responsePayload, e);
            handleResponseError(responsePayload, e);
        }
    }

    /**
     * ✅ Listen for saved4later error responses
     */
    @KafkaListener(
            topics = "saved4later.error",
            groupId = "gateway-bff-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSaved4LaterError(ConsumerRecord<String, Object> record) {
        Object errorPayload = record.value();
        log.error("Received saved4later error response: {}", errorPayload);
        handleErrorResponse(errorPayload, "Saved4later service error");
    }

    /**
     * ✅ Convert saved4later service response to SavedItemsResponseDTO
     */
    private SavedItemsResponseDTO convertToSavedItemsResponse(Object savedItemsData) {
        try {
            log.info("Converting saved4later data: {}", savedItemsData);

            Map<String, Object> dataMap = convertToMap(savedItemsData);

            // Extract basic info with proper null checks
            String userIdStr = extractStringValue(dataMap, "userId");
            Number itemCountNum = (Number) dataMap.get("itemCount");

            log.info("Extracted saved4later fields - userId: '{}', itemCount: {}",
                    userIdStr, itemCountNum);

            // Parse UUID
            UUID userId = parseUUID(userIdStr, "userId");

            // Convert item count
            Integer itemCount = convertToInteger(itemCountNum, "itemCount");

            // Extract and convert items
            List<SavedItemDTO> savedItems = new ArrayList<>();
            Object itemsObj = dataMap.get("items");

            if (itemsObj instanceof List) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                itemCount = items.size(); // Update with actual count

                log.info("Saved4later contains {} items", itemCount);

                if (!items.isEmpty()) {
                    for (Map<String, Object> item : items) {
                        try {
                            SavedItemDTO savedItem = convertSavedItem(item);
                            if (savedItem != null) {
                                savedItems.add(savedItem);
                            }
                        } catch (Exception e) {
                            log.error("Error converting saved item: {}", item, e);
                            // Continue processing other items
                        }
                    }

                    log.info("Successfully processed {} saved items", savedItems.size());
                } else {
                    log.info("Saved4later is empty - no items to process");
                }
            } else {
                log.warn("Items object is not a List, type: {}, value: {}",
                        itemsObj != null ? itemsObj.getClass().getSimpleName() : "null", itemsObj);
            }

            // Parse datetime fields
            LocalDateTime lastUpdated = parseDateTime(dataMap.get("lastUpdated"));
            if (lastUpdated == null) {
                lastUpdated = LocalDateTime.now();
            }

            // Build the response
            SavedItemsResponseDTO result = SavedItemsResponseDTO.builder()
                    .userId(userId)
                    .items(savedItems)
                    .itemCount(savedItems.size())
                    .lastUpdated(lastUpdated)
                    .build();

            log.info("✅ FINAL SAVED4LATER RESPONSE: userId={}, itemCount={}, lastUpdated={}",
                    result.getUserId(), result.getItemCount(), result.getLastUpdated());

            return result;

        } catch (Exception e) {
            log.error("Error converting saved4later data to response: {}", savedItemsData, e);
            return createFallbackSavedItemsResponse();
        }
    }

    /**
     * ✅ Convert saved item to SavedItemDTO
     */
    private SavedItemDTO convertSavedItem(Map<String, Object> item) {
        try {
            log.debug("Converting saved item: {}", item);

            // Extract item fields
            String itemIdStr = extractStringValue(item, "id");
            String userIdStr = extractStringValue(item, "userId");
            String productIdStr = extractStringValue(item, "productId");

            log.debug("Item field extraction - id: '{}', userId: '{}', productId: '{}'",
                    itemIdStr, userIdStr, productIdStr);

            // Parse UUIDs
            UUID itemId = parseUUID(itemIdStr, "itemId");
            UUID userId = parseUUID(userIdStr, "userId");
            UUID productId = parseUUID(productIdStr, "productId");

            // Parse datetime
            LocalDateTime savedAt = parseDateTime(item.get("savedAt"));

            SavedItemDTO savedItem = SavedItemDTO.builder()
                    .id(itemId)
                    .userId(userId)
                    .productId(productId)
                    .savedAt(savedAt)
                    .build();

            log.debug("✅ Converted saved item: id={}, userId={}, productId={}, savedAt={}",
                    savedItem.getId(), savedItem.getUserId(), savedItem.getProductId(), savedItem.getSavedAt());

            return savedItem;

        } catch (Exception e) {
            log.error("Error converting saved item: {}", item, e);
            return createFallbackSavedItem();
        }
    }

    /**
     * ✅ Helper methods for type conversion (reusing from existing consumers)
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
     * ✅ Enhanced datetime parsing
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

        return null;
    }

    private SavedItemDTO createFallbackSavedItem() {
        return SavedItemDTO.builder()
                .id(null)
                .userId(null)
                .productId(null)
                .savedAt(null)
                .build();
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
     * ✅ Create fallback response
     */
    private SavedItemsResponseDTO createFallbackSavedItemsResponse() {
        return SavedItemsResponseDTO.builder()
                .userId(null)
                .items(List.of())
                .itemCount(0)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}