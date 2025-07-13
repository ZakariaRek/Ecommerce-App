package com.Ecommerce.Cart.Service.Lisiteners.AsyncComm;

import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Response.CartItemResponse;
import com.Ecommerce.Cart.Service.Payload.Response.ShoppingCartResponse;
import com.Ecommerce.Cart.Service.Payload.kafka.CartRequestDTO;
import com.Ecommerce.Cart.Service.Payload.kafka.CartResponseDTO;
import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import com.Ecommerce.Cart.Service.Repositories.ShoppingCartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartKafkaEventHandler {

    private final ShoppingCartService cartService;
    private final ShoppingCartRepository cartRepository; // ✅ Add direct repository access
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "cart.request",
            groupId = "cart-service-group",
            containerFactory = "simpleKafkaListenerContainerFactory"
    )
    public void handleCartRequest(@Payload String messagePayload) {
        log.info("Received cart request message: {}", messagePayload);

        try {
            CartRequestDTO request = objectMapper.readValue(messagePayload, CartRequestDTO.class);

            log.info("Parsed cart request for user: {} with correlationId: {}",
                    request.getUserId(), request.getCorrelationId());

            // Handle UUID parsing exactly like the controller
            UUID parsedUserId = parseUUID(request.getUserId());

            // ✅ ADD DEBUGGING: Log the parsed UUID
            log.info("Original userId: '{}', Parsed UUID: '{}'", request.getUserId(), parsedUserId);

            // ✅ FIXED: Check if cart exists BEFORE calling getOrCreateCart
            Optional<ShoppingCart> existingCart = cartRepository.findByUserId(parsedUserId);

            if (existingCart.isPresent()) {
                log.info("Found existing cart with {} items for user: {}",
                        existingCart.get().getItems().size(), parsedUserId);
            } else {
                log.warn("No existing cart found for user: {}. Will create new cart.", parsedUserId);

                // ✅ DEBUGGING: Let's also check with different UUID formats
                log.info("Debugging: Checking different UUID formats in database...");
                debugDatabaseSearch(request.getUserId());
            }

            // ✅ Use the existing cart or get/create new one
            ShoppingCart cart = existingCart.orElseGet(() -> {
                log.info("Creating new cart for user: {}", parsedUserId);
                return cartService.getOrCreateCart(parsedUserId);
            });

            // ✅ Log cart details before mapping
            log.info("Cart details - ID: {}, UserId: {}, Items: {}, Total: {}",
                    cart.getId(), cart.getUserId(),
                    cart.getItems() != null ? cart.getItems().size() : 0,
                    cart.calculateTotal());

            ShoppingCartResponse cartData = mapToCartResponse(cart);

            CartResponseDTO response = CartResponseDTO.builder()
                    .correlationId(request.getCorrelationId())
                    .success(true)
                    .message("Cart retrieved successfully")
                    .data(cartData)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send("cart.response", request.getCorrelationId(), response);
            log.info("Sent cart response for correlationId: {} with {} items",
                    request.getCorrelationId(), cartData.getItems().size());

        } catch (Exception e) {
            log.error("Error processing cart request from message: {}", messagePayload, e);

            String correlationId = extractCorrelationId(messagePayload);

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
     * ✅ DEBUGGING METHOD: Check different UUID formats in database
     */
    private void debugDatabaseSearch(String originalUserId) {
        try {
            // Check original format
            try {
                UUID original = UUID.fromString(originalUserId);
                Optional<ShoppingCart> cart1 = cartRepository.findByUserId(original);
                log.debug("Search with original UUID '{}': {}", original, cart1.isPresent() ? "FOUND" : "NOT FOUND");
            } catch (Exception e) {
                log.debug("Original format '{}' is not a valid UUID", originalUserId);
            }

            // Check padded format
            String cleaned = originalUserId.replaceAll("-", "");
            if (cleaned.length() == 24) {
                String padded = cleaned + "00000000";
                String formatted = padded.substring(0, 8) + "-" +
                        padded.substring(8, 12) + "-" +
                        padded.substring(12, 16) + "-" +
                        padded.substring(16, 20) + "-" +
                        padded.substring(20, 32);
                UUID paddedUuid = UUID.fromString(formatted);
                Optional<ShoppingCart> cart2 = cartRepository.findByUserId(paddedUuid);
                log.debug("Search with padded UUID '{}': {}", paddedUuid, cart2.isPresent() ? "FOUND" : "NOT FOUND");
            }

            // ✅ NEW: Check all carts in database to see what UUIDs actually exist
            List<ShoppingCart> allCarts = cartRepository.findAll();
            log.debug("Total carts in database: {}", allCarts.size());
            for (ShoppingCart cart : allCarts) {
                log.debug("Cart userId in DB: '{}'", cart.getUserId());

                // Check if this matches our parsed UUID in any way
                if (cart.getUserId().toString().contains(cleaned.substring(0, 8))) {
                    log.warn("POTENTIAL MATCH FOUND: DB userId '{}' contains part of requested '{}'",
                            cart.getUserId(), originalUserId);
                }
            }

        } catch (Exception e) {
            log.error("Error during database debugging", e);
        }
    }

    /**
     * ✅ Enhanced parseUUID with more logging
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
     * ✅ Enhanced correlationId extraction
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

            CartRequestDTO partialRequest = objectMapper.readValue(messagePayload, CartRequestDTO.class);
            return partialRequest.getCorrelationId();

        } catch (Exception extractionError) {
            log.warn("Failed to extract correlationId from message: {}", extractionError.getMessage());
            return "unknown-" + System.currentTimeMillis();
        }
    }

    /**
     * ✅ Same mapping method as controller
     */
    private ShoppingCartResponse mapToCartResponse(ShoppingCart cart) {
        ShoppingCartResponse response = ShoppingCartResponse.builder()
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

        log.debug("Mapped cart response: {} items, total: {}",
                response.getItems().size(), response.getTotal());

        return response;
    }
}