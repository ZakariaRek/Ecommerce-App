package com.Ecommerce.Gateway_Service.Service;

import com.Ecommerce.Gateway_Service.DTOs.*;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.CartRequestDTO;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.CartResponseDTO;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.ProductBatchRequestEventDTO;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.ProductBatchResponseDTO;
import com.Ecommerce.Gateway_Service.Kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncCartBffService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AsyncResponseManager responseManager;
    private final ObjectMapper objectMapper;

    // ✅ FULLY ASYNC WITH KAFKA
    public Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> getEnrichedCart(String userId) {
        log.info("Starting async enriched cart fetch for user: {}", userId);

        // ✅ Parse and normalize the userId before sending to Cart service
        String normalizedUserId;
        try {
            UUID parsedUserId = parseUUID(userId);
            normalizedUserId = parsedUserId.toString();
            log.debug("Normalized user ID '{}' to UUID: {}", userId, normalizedUserId);
        } catch (Exception e) {
            log.error("Failed to parse user ID: {}", userId, e);
            return Mono.just(createEmptyEnrichedCart(userId));
        }

        String correlationId = UUID.randomUUID().toString();

        return getCartFromServiceAsync(normalizedUserId, correlationId) // ✅ Use normalized UUID
                .flatMap(cartResponse -> {
                    if (cartResponse.getData() == null ||
                            cartResponse.getData().getItems() == null ||
                            cartResponse.getData().getItems().isEmpty()) {

                        log.info("Empty cart for user: {}", normalizedUserId);
                        return Mono.just(createEmptyEnrichedCart(normalizedUserId));
                    }

                    List<UUID> productIds = cartResponse.getData().getItems().stream()
                            .map(CartItemDTO::getProductId)
                            .collect(Collectors.toList());

                    log.info("Found {} unique products in cart for user: {}", productIds.size(), normalizedUserId);

                    return getProductInfoBatchAsync(productIds)
                            .map(productInfos -> mergeCartWithProductInfo(cartResponse.getData(), productInfos));
                })
                .timeout(Duration.ofSeconds(15))
                .doOnError(error -> log.error("Error in async enriched cart fetch for user {}: {}", normalizedUserId, error.getMessage()))
                .onErrorReturn(createEmptyEnrichedCart(normalizedUserId));
    }

    /**
     * ✅ Add the same parseUUID method as Cart service
     */
    private UUID parseUUID(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            throw new IllegalArgumentException("UUID string cannot be null or empty");
        }

        try {
            // First, try the standard parsing logic
            return parseUUIDStandard(uuidString);
        } catch (IllegalArgumentException e) {
            // If that fails, check if it's Base64 encoded
            if (isBase64Encoded(uuidString)) {
                log.debug("Detected Base64 format, converting to deterministic UUID");
                return generateDeterministicUUID(uuidString);
            }
            // If not Base64, rethrow the original exception
            throw e;
        }
    }

    /**
     * ✅ Standard UUID parsing logic (same as Cart service)
     */
    private UUID parseUUIDStandard(String uuidString) {
        // Remove any existing hyphens
        String cleanUuid = uuidString.replaceAll("-", "");

        // Handle MongoDB ObjectId (24 characters) by padding to UUID format
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

        // Try parsing as-is (in case it's already properly formatted)
        return UUID.fromString(uuidString);
    }

    /**
     * ✅ Check if string is Base64 encoded
     */
    private boolean isBase64Encoded(String str) {
        try {
            if (!str.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                return false;
            }
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * ✅ Generate deterministic UUID from any string using SHA-256
     */
    private UUID generateDeterministicUUID(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Take first 16 bytes for UUID
            byte[] uuidBytes = new byte[16];
            System.arraycopy(hash, 0, uuidBytes, 0, 16);

            // Set version (4) and variant bits for UUID v4
            uuidBytes[6] &= 0x0f;  // Clear version
            uuidBytes[6] |= 0x40;  // Set version to 4
            uuidBytes[8] &= 0x3f;  // Clear variant
            uuidBytes[8] |= 0x80;  // Set to IETF variant

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : uuidBytes) {
                hexString.append(String.format("%02x", b));
            }

            // Format as UUID
            String hex = hexString.toString();
            String formattedUuid = hex.substring(0, 8) + "-" +
                    hex.substring(8, 12) + "-" +
                    hex.substring(12, 16) + "-" +
                    hex.substring(16, 20) + "-" +
                    hex.substring(20, 32);

            return UUID.fromString(formattedUuid);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate deterministic UUID for: " + input, e);
        }
    }

    // ✅ Rest of your methods remain the same...
    private Mono<CartServiceResponseDTO> getCartFromServiceAsync(String userId, String correlationId) {
        CartRequestDTO request = CartRequestDTO.builder()
                .correlationId(correlationId)
                .userId(userId) // This is now a normalized UUID string
                .timestamp(System.currentTimeMillis())
                .build();

        // Send request to Kafka
        kafkaTemplate.send(KafkaTopics.CART_REQUEST, correlationId, request);
        log.debug("Sent cart request for user: {} with correlationId: {}", userId, correlationId);

        // Wait for response
        return responseManager.waitForResponse(correlationId, Duration.ofSeconds(5), CartResponseDTO.class)
                .map(cartResponse -> {
                    if (cartResponse.isSuccess()) {
                        CartServiceResponseDTO response = new CartServiceResponseDTO();
                        response.setSuccess(true);
                        response.setData(cartResponse.getData().getData());
                        return response;
                    } else {
                        log.warn("Cart service returned error: {}", cartResponse.getMessage());
                        return createEmptyCartServiceResponse(userId);
                    }
                })
                .onErrorReturn(createEmptyCartServiceResponse(userId));
    }


    private Mono<List<ProductBatchInfoDTO>> getProductInfoBatchAsync(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Mono.just(List.of());
        }

        String correlationId = UUID.randomUUID().toString();

        ProductBatchRequestEventDTO request = ProductBatchRequestEventDTO.builder()
                .correlationId(correlationId)
                .productIds(productIds)
                .timestamp(System.currentTimeMillis())
                .build();

        // Send request to Kafka
        kafkaTemplate.send(KafkaTopics.PRODUCT_BATCH_REQUEST, correlationId, request);
        log.debug("Sent product batch request for {} products with correlationId: {}", productIds.size(), correlationId);

        // Wait for response
        return responseManager.waitForResponse(correlationId, Duration.ofSeconds(8), ProductBatchResponseDTO.class)
                .map(response -> {
                    if (response.isSuccess()) {
                        return response.getProducts();
                    } else {
                        log.warn("Product service returned error for correlationId: {}", correlationId);
                        return List.<ProductBatchInfoDTO>of();
                    }
                })
                .onErrorReturn(List.of());
    }

    // Kafka Listeners for Responses
    @KafkaListener(topics = KafkaTopics.CART_RESPONSE)
    public void handleCartResponse(@Payload CartResponseDTO response) {
        log.debug("Received cart response for correlationId: {}", response.getCorrelationId());
        responseManager.completeRequest(response.getCorrelationId(), response);
    }

    @KafkaListener(topics = KafkaTopics.PRODUCT_BATCH_RESPONSE)
    public void handleProductBatchResponse(@Payload ProductBatchResponseDTO response) {
        log.debug("Received product batch response for correlationId: {}", response.getCorrelationId());
        responseManager.completeRequest(response.getCorrelationId(), response);
    }

    @KafkaListener(topics = KafkaTopics.CART_ERROR)
    public void handleCartError(@Payload Map<String, Object> errorPayload) {
        String correlationId = (String) errorPayload.get("correlationId");
        String errorMessage = (String) errorPayload.get("message");
        log.error("Received cart error for correlationId: {}, message: {}", correlationId, errorMessage);
        responseManager.completeRequestExceptionally(correlationId, new RuntimeException(errorMessage));
    }

    @KafkaListener(topics = KafkaTopics.PRODUCT_ERROR)
    public void handleProductError(@Payload Map<String, Object> errorPayload) {
        String correlationId = (String) errorPayload.get("correlationId");
        String errorMessage = (String) errorPayload.get("message");
        log.error("Received product error for correlationId: {}, message: {}", correlationId, errorMessage);
        responseManager.completeRequestExceptionally(correlationId, new RuntimeException(errorMessage));
    }

    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO mergeCartWithProductInfo(ShoppingCartDTO cart, List<ProductBatchInfoDTO> productInfos) {
        log.info("Merging cart data with product information");

        Map<UUID, ProductBatchInfoDTO> productMap = productInfos.stream()
                .collect(Collectors.toMap(ProductBatchInfoDTO::getId, p -> p));

        List<EnrichedCartItemDTO> enrichedItems = cart.getItems().stream()
                .map(cartItem -> enrichCartItem(cartItem, productMap.get(cartItem.getProductId())))
                .collect(Collectors.toList());

        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(enrichedItems)
                .total(cart.getTotal())
                .itemCount(enrichedItems.size())
                .totalQuantity(enrichedItems.stream().mapToInt(EnrichedCartItemDTO::getQuantity).sum())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expiresAt(cart.getExpiresAt())
                .build();
    }

    private EnrichedCartItemDTO enrichCartItem(CartItemDTO cartItem, ProductBatchInfoDTO productInfo) {
        EnrichedCartItemDTO.EnrichedCartItemDTOBuilder builder = EnrichedCartItemDTO.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProductId())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .subtotal(cartItem.getSubtotal())
                .addedAt(cartItem.getAddedAt());

        if (productInfo != null) {
            builder
                    .productName(productInfo.getName())
                    .productImage(productInfo.getImagePath())
                    .inStock(productInfo.getInStock())
                    .availableQuantity(productInfo.getAvailableQuantity())
                    .productStatus(productInfo.getStatus().toString());
        } else {
            builder
                    .productName("Product Not Found")
                    .productImage("/api/products/images/not-found.png")
                    .inStock(false)
                    .availableQuantity(0)
                    .productStatus("NOT_FOUND");
        }

        return builder.build();
    }


    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO createEmptyEnrichedCart(String userId) {
        UUID parsedUserId;
        try {
            // Try to parse as UUID first
            parsedUserId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // If it's not a valid UUID (like MongoDB ObjectId), generate a random UUID
            parsedUserId = UUID.randomUUID();
            log.warn("Invalid UUID format for userId: {}, using random UUID", userId);
        }

        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .userId(parsedUserId)
                .items(List.of())
                .total(java.math.BigDecimal.ZERO)
                .itemCount(0)
                .totalQuantity(0)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }


    private CartServiceResponseDTO createEmptyCartServiceResponse(String userId) {
        CartServiceResponseDTO response = new CartServiceResponseDTO();
        response.setSuccess(false);
        response.setMessage("Cart service unavailable");

        ShoppingCartDTO emptyCart = new ShoppingCartDTO();

        // Handle UUID parsing safely
        try {
            emptyCart.setUserId(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            // If it's not a valid UUID (like MongoDB ObjectId), generate a random UUID
            emptyCart.setUserId(UUID.randomUUID());
            log.warn("Invalid UUID format for userId: {}, using random UUID", userId);
        }

        emptyCart.setItems(List.of());
        emptyCart.setTotal(java.math.BigDecimal.ZERO);
        emptyCart.setCreatedAt(java.time.LocalDateTime.now());
        emptyCart.setUpdatedAt(java.time.LocalDateTime.now());

        response.setData(emptyCart);
        return response;
    }
}