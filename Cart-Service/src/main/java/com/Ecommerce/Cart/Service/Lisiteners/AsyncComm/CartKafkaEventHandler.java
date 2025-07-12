package com.Ecommerce.Cart.Service.Lisiteners.AsyncComm;

import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Response.CartItemResponse;
import com.Ecommerce.Cart.Service.Payload.Response.ShoppingCartResponse;
import com.Ecommerce.Cart.Service.Payload.kafka.CartRequestDTO;
import com.Ecommerce.Cart.Service.Payload.kafka.CartResponseDTO;
import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartKafkaEventHandler {

    private final ShoppingCartService cartService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper; // ✅ Add ObjectMapper

    @KafkaListener(topics = "cart.request", groupId = "cart-service-group")
    public void handleCartRequest(@Payload String messagePayload) { // ✅ Use String instead of CartRequestDTO
        log.info("Received cart request message: {}", messagePayload);

        try {
            // ✅ Manual deserialization
            CartRequestDTO request = objectMapper.readValue(messagePayload, CartRequestDTO.class);

            log.info("Parsed cart request for user: {} with correlationId: {}",
                    request.getUserId(), request.getCorrelationId());

            // Handle UUID parsing exactly like the controller
            UUID parsedUserId = parseUUID(request.getUserId());

            // Use the same method as controller: getOrCreateCart
            ShoppingCart cart = cartService.getOrCreateCart(parsedUserId);

            // Use the same mapping method as controller
            ShoppingCartResponse cartData = mapToCartResponse(cart);

            CartResponseDTO response = CartResponseDTO.builder()
                    .correlationId(request.getCorrelationId())
                    .success(true)
                    .message("Cart retrieved successfully")
                    .data(cartData)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send("cart.response", request.getCorrelationId(), response);
            log.info("Sent cart response for correlationId: {}", request.getCorrelationId());

        } catch (Exception e) {
            log.error("Error processing cart request from message: {}", messagePayload, e);

            // Try to extract correlationId for error response
            String correlationId = "unknown";
            try {
                // Simple regex to extract correlationId from JSON string
                if (messagePayload.contains("correlationId")) {
                    // This is a basic extraction - could be improved
                    String[] parts = messagePayload.split("\"correlationId\"\\s*:\\s*\"");
                    if (parts.length > 1) {
                        String[] endParts = parts[1].split("\"");
                        if (endParts.length > 0) {
                            correlationId = endParts[0];
                        }
                    }
                }
            } catch (Exception extractionError) {
                log.warn("Failed to extract correlationId from error message");
            }

            CartResponseDTO errorResponse = CartResponseDTO.builder()
                    .correlationId(correlationId)
                    .success(false)
                    .message("Failed to process cart request: " + e.getMessage())
                    .data(null)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send("cart.error", correlationId, errorResponse);
        }
    }

    /**
     * ✅ Exact same parseUUID method as controller
     */
    private UUID parseUUID(String uuidString) {
        // Remove any existing hyphens
        String cleanUuid = uuidString.replaceAll("-", "");
        if (cleanUuid.length() == 24 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Pad with zeros to make it 32 characters
            cleanUuid = cleanUuid + "00000000";
        }

        // Check if it's exactly 32 hex characters
        if (cleanUuid.length() == 32 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Insert hyphens at correct positions: 8-4-4-4-12
            String formattedUuid = cleanUuid.substring(0, 8) + "-" +
                    cleanUuid.substring(8, 12) + "-" +
                    cleanUuid.substring(12, 16) + "-" +
                    cleanUuid.substring(16, 20) + "-" +
                    cleanUuid.substring(20, 32);
            return UUID.fromString(formattedUuid);
        }
        return UUID.fromString(uuidString);
    }

    /**
     * ✅ Exact same mapping method as controller
     */
    private ShoppingCartResponse mapToCartResponse(ShoppingCart cart) {
        return ShoppingCartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(cart.getItems() != null ? cart.getItems().stream()
                        .map(item -> CartItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .subtotal(item.getSubtotal())
                                .addedAt(item.getAddedAt())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .total(cart.calculateTotal())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expiresAt(cart.getExpiresAt())
                .build();
    }
}