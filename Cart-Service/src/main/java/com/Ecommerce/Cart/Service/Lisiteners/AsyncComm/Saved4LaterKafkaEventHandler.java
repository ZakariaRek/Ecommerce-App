// Cart-Service/src/main/java/com/Ecommerce/Cart/Service/Lisiteners/AsyncComm/Saved4LaterKafkaEventHandler.java
package com.Ecommerce.Cart.Service.Lisiteners.AsyncComm;

import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Payload.Response.SavedItemResponse;
import com.Ecommerce.Cart.Service.Payload.kafka.Saved4LaterRequestDTO;
import com.Ecommerce.Cart.Service.Payload.kafka.Saved4LaterResponseDTO;
import com.Ecommerce.Cart.Service.Services.SavedForLaterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class Saved4LaterKafkaEventHandler {

    private final SavedForLaterService savedForLaterService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "saved4later.request",
            groupId = "cart-service-group",
            containerFactory = "simpleKafkaListenerContainerFactory"
    )
    public void handleSaved4LaterRequest(@Payload String messagePayload) {
        log.info("Received saved4later request message: {}", messagePayload);

        try {
            Saved4LaterRequestDTO request = objectMapper.readValue(messagePayload, Saved4LaterRequestDTO.class);

            log.info("Parsed saved4later request for user: {} with correlationId: {}",
                    request.getUserId(), request.getCorrelationId());

            // Handle UUID parsing exactly like the cart controller
            UUID parsedUserId = parseUUID(request.getUserId());

            log.info("Original userId: '{}', Parsed UUID: '{}'", request.getUserId(), parsedUserId);

            // Get saved items from service
            List<SavedForLater> savedItems = savedForLaterService.getSavedItems(parsedUserId);

            log.info("Found {} saved items for user: {}", savedItems.size(), parsedUserId);

            // Map to response DTOs
            List<SavedItemResponse> savedItemResponses = savedItems.stream()
                    .map(this::mapToSavedItemResponse)
                    .collect(Collectors.toList());

            // Create the data object that matches the expected structure
            SavedItemsDataDTO savedItemsData = SavedItemsDataDTO.builder()
                    .userId(parsedUserId)
                    .items(savedItemResponses)
                    .itemCount(savedItemResponses.size())
                    .lastUpdated(LocalDateTime.now())
                    .build();

            // Create response
            Saved4LaterResponseDTO response = Saved4LaterResponseDTO.builder()
                    .correlationId(request.getCorrelationId())
                    .success(true)
                    .message("Saved items retrieved successfully")
                    .data(savedItemsData)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send("saved4later.response", request.getCorrelationId(), response);
            log.info("Sent saved4later response for correlationId: {} with {} items",
                    request.getCorrelationId(), savedItemResponses.size());

        } catch (Exception e) {
            log.error("Error processing saved4later request from message: {}", messagePayload, e);

            String correlationId = extractCorrelationId(messagePayload);

            Saved4LaterResponseDTO errorResponse = Saved4LaterResponseDTO.builder()
                    .correlationId(correlationId)
                    .success(false)
                    .message("Failed to process saved4later request: " + e.getMessage())
                    .data(null)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send("saved4later.error", correlationId, errorResponse);
        }
    }

    /**
     * Parse UUID with the same logic as the cart controller
     */
    private UUID parseUUID(String uuidString) {
        try {
            log.debug("Parsing UUID from: '{}'", uuidString);

            // Remove any existing hyphens
            String cleanUuid = uuidString.replaceAll("-", "");
            log.debug("Cleaned UUID: '{}'", cleanUuid);

            // Handle MongoDB ObjectId (24 characters) by padding to UUID format
            if (cleanUuid.length() == 24 && cleanUuid.matches("[0-9a-fA-F]+")) {
                // Pad with zeros to make it 32 characters
                cleanUuid = cleanUuid + "00000000";
                log.debug("Padded UUID: '{}'", cleanUuid);
            }

            // Check if it's exactly 32 hex characters
            if (cleanUuid.length() == 32 && cleanUuid.matches("[0-9a-fA-F]+")) {
                // Insert hyphens at correct positions: 8-4-4-4-12
                String formattedUuid = cleanUuid.substring(0, 8) + "-" +
                        cleanUuid.substring(8, 12) + "-" +
                        cleanUuid.substring(12, 16) + "-" +
                        cleanUuid.substring(16, 20) + "-" +
                        cleanUuid.substring(20, 32);

                UUID result = UUID.fromString(formattedUuid);
                log.debug("Final formatted UUID: '{}'", result);
                return result;
            }

            // Try parsing as-is (in case it's already properly formatted)
            UUID result = UUID.fromString(uuidString);
            log.debug("Parsed UUID as-is: '{}'", result);
            return result;

        } catch (Exception e) {
            log.error("Failed to parse UUID: '{}'", uuidString, e);
            throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
        }
    }

    /**
     * Extract correlationId from message payload
     */
    private String extractCorrelationId(String messagePayload) {
        try {
            if (messagePayload.contains("correlationId")) {
                String[] parts = messagePayload.split("\"correlationId\"\\s*:\\s*\"");
                if (parts.length > 1) {
                    String[] endParts = parts[1].split("\"");
                    if (endParts.length > 0) {
                        return endParts[0];
                    }
                }
            }

            Saved4LaterRequestDTO partialRequest = objectMapper.readValue(messagePayload, Saved4LaterRequestDTO.class);
            return partialRequest.getCorrelationId();

        } catch (Exception extractionError) {
            log.warn("Failed to extract correlationId from message: {}", extractionError.getMessage());
            return "unknown-" + System.currentTimeMillis();
        }
    }

    /**
     * Map SavedForLater to SavedItemResponse
     */
    private SavedItemResponse mapToSavedItemResponse(SavedForLater savedItem) {
        return SavedItemResponse.builder()
                .id(savedItem.getId())
                .productId(savedItem.getProductId())
                .savedAt(savedItem.getSavedAt())
                .build();
    }

    /**
     * Inner DTO class for the data structure
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SavedItemsDataDTO {
        private UUID userId;
        private List<SavedItemResponse> items;
        private Integer itemCount;
        private LocalDateTime lastUpdated;
    }
}
