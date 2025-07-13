package com.Ecommerce.Gateway_Service.Service;

import com.Ecommerce.Gateway_Service.DTOs.EnrichedCartItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.EnrichedShoppingCartResponse;
import com.Ecommerce.Gateway_Service.DTOs.ProductBatchInfoDTO;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.ProductBatchResponseDTO;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncCartBffService {

    private final KafkaTemplate<String, Object> gatewayKafkaTemplate;
    private final AsyncResponseManager asyncResponseManager;
    private final AsyncProductService asyncProductService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Get enriched cart using async Kafka communication - WITHOUT product enrichment
     */
    public Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> getEnrichedCart(String userId) {
        String correlationId = UUID.randomUUID().toString();

        log.info("Starting async cart request for userId: {} with correlationId: {}", userId, correlationId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.error("Invalid userId provided: {}", userId);
                return Mono.just(createEmptyCartResponse(userId));
            }

            // Create cart request
            Map<String, Object> cartRequest = new HashMap<>();
            cartRequest.put("correlationId", correlationId);
            cartRequest.put("userId", userId);
            cartRequest.put("timestamp", System.currentTimeMillis());

            log.info("🔍 SERVICE: Sending cart request to Kafka: {}", cartRequest);

            // Send request to cart service
            gatewayKafkaTemplate.send("cart.request", correlationId, cartRequest);

            // Wait for response with timeout
            Duration timeout = Duration.ofSeconds(30);

            return asyncResponseManager.waitForResponse(
                            correlationId,
                            timeout,
                            EnrichedShoppingCartResponse.EnrichedCartResponseDTO.class
                    )
                    .doOnSuccess(response -> {
                        log.info("🔍 SERVICE: Successfully received async cart response for correlationId: {} with {} items",
                                correlationId, response.getItemCount());
                        log.info("🔍 SERVICE: Cart response details - id: {}, userId: {}, total: {}, itemCount: {}, totalQuantity: {}",
                                response.getId(), response.getUserId(), response.getTotal(),
                                response.getItemCount(), response.getTotalQuantity());
                    })
                    .doOnError(error -> {
                        log.error("Failed to get async cart response for correlationId: {}", correlationId, error);
                    })
                    .onErrorReturn(createEmptyCartResponse(userId));

        } catch (Exception e) {
            log.error("Error initiating async cart request for userId: {}", userId, e);
            return Mono.just(createEmptyCartResponse(userId));
        }
    }

    /**
     * ✅ Get enriched cart WITH product enrichment - FIXED version
     */
    public Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> getEnrichedCartWithProducts(String userId) {
        log.info("🔍 SERVICE: Starting enriched cart with products for userId: {}", userId);

        return getEnrichedCart(userId)
                .flatMap(cartResponse -> {
                    log.info("🔍 SERVICE: Received cart response before product enrichment:");
                    log.info("   - Cart ID: {}", cartResponse.getId());
                    log.info("   - User ID: {}", cartResponse.getUserId());
                    log.info("   - Total: {}", cartResponse.getTotal());
                    log.info("   - Item Count: {}", cartResponse.getItemCount());
                    log.info("   - Total Quantity: {}", cartResponse.getTotalQuantity());
                    log.info("   - Created At: {}", cartResponse.getCreatedAt());
                    log.info("   - Updated At: {}", cartResponse.getUpdatedAt());
                    log.info("   - Expires At: {}", cartResponse.getExpiresAt());

                    if (cartResponse.getItemCount() != null && cartResponse.getItemCount() > 0) {
                        // Extract product IDs from cart items
                        List<UUID> productIds = cartResponse.getItems().stream()
                                .map(EnrichedCartItemDTO::getProductId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        if (!productIds.isEmpty()) {
                            log.info("🔍 SERVICE: Enriching {} cart items with product data", productIds.size());
                            log.info("🔍 SERVICE: Product IDs to fetch: {}", productIds);

                            // ✅ FIXED: Handle the correct return type from AsyncProductService
                            return asyncProductService.getProductsBatch(productIds)
                                    .map(productResponse -> {
                                        log.info("🔍 SERVICE: Received product response");
                                        log.info("🔍 SERVICE: Response type: {}", productResponse.getClass().getSimpleName());

                                        // ✅ Your AsyncProductService returns List<EnrichedCartItemDTO>, not ProductBatchResponseDTO
                                        @SuppressWarnings("unchecked")
                                        List<EnrichedCartItemDTO> productItems = (List<EnrichedCartItemDTO>) productResponse;

                                        log.info("🔍 SERVICE: Successfully cast to List<EnrichedCartItemDTO> with {} items", productItems.size());

                                        // Log the product details we received
                                        for (EnrichedCartItemDTO product : productItems) {
                                            log.info("🔍 SERVICE: Product: id={}, name={}, status={}, inStock={}, availableQuantity={}",
                                                    product.getProductId(), product.getProductName(),
                                                    product.getProductStatus(), product.getInStock(), product.getAvailableQuantity());
                                        }

                                        // ✅ Use the correct merge method for List<EnrichedCartItemDTO>
                                        return mergeCartWithProductItems(cartResponse, productItems);
                                    })
                                    .doOnSuccess(enrichedCart -> {
                                        log.info("🔍 SERVICE: Successfully enriched cart with product details");
                                        log.info("🔍 SERVICE: Final enriched cart - id: {}, userId: {}, total: {}, itemCount: {}",
                                                enrichedCart.getId(), enrichedCart.getUserId(),
                                                enrichedCart.getTotal(), enrichedCart.getItemCount());
                                    })
                                    .onErrorResume(error -> {
                                        log.error("🔍 SERVICE: Error enriching cart with products", error);
                                        return Mono.just(cartResponse); // Return cart without enrichment on error
                                    });
                        }
                    }

                    log.info("🔍 SERVICE: Cart has no items to enrich, returning basic cart data");
                    return Mono.just(cartResponse);
                });
    }

    /**
     * ✅ NEW: Merge cart data with product items (for AsyncProductService that returns List<EnrichedCartItemDTO>)
     */
    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO mergeCartWithProductItems(
            EnrichedShoppingCartResponse.EnrichedCartResponseDTO cartResponse,
            List<EnrichedCartItemDTO> productItems) {

        log.info("🔍 SERVICE: Merging cart with {} cart items and {} product items",
                cartResponse.getItemCount(), productItems.size());

        // Create a map of productId -> product details for quick lookup
        Map<UUID, EnrichedCartItemDTO> productMap = productItems.stream()
                .collect(Collectors.toMap(
                        EnrichedCartItemDTO::getProductId,
                        product -> product,
                        (existing, replacement) -> existing // Handle duplicates
                ));

        log.info("🔍 SERVICE: Created product map with {} entries", productMap.size());

        // Enrich cart items with product details
        List<EnrichedCartItemDTO> enrichedItems = cartResponse.getItems().stream()
                .map(cartItem -> {
                    EnrichedCartItemDTO productDetail = productMap.get(cartItem.getProductId());

                    if (productDetail != null) {
                        log.debug("🔍 SERVICE: Enriching cart item {} with product details", cartItem.getProductId());

                        // ✅ Merge ALL cart item data with product details
                        return EnrichedCartItemDTO.builder()
                                // ✅ PRESERVE ALL CART-SPECIFIC DATA
                                .id(cartItem.getId())                           // Cart item ID
                                .productId(cartItem.getProductId())             // Product ID
                                .quantity(cartItem.getQuantity())               // Quantity in cart
                                .price(cartItem.getPrice())                     // Price from cart
                                .subtotal(cartItem.getSubtotal())               // Subtotal from cart
                                .addedAt(cartItem.getAddedAt())                 // When added to cart

                                // ✅ ADD PRODUCT DETAILS FROM PRODUCT SERVICE
                                .productName(productDetail.getProductName())           // Product name
                                .productImage(productDetail.getProductImage())        // Product image
                                .productStatus(productDetail.getProductStatus())      // Product status
                                .inStock(productDetail.getInStock())                  // Stock status
                                .availableQuantity(productDetail.getAvailableQuantity()) // Available quantity
                                .build();
                    } else {
                        // Product details not found, keep cart item as is with fallback product info
                        log.warn("🔍 SERVICE: Product details not found for productId: {}", cartItem.getProductId());
                        return EnrichedCartItemDTO.builder()
                                // ✅ PRESERVE ALL CART DATA
                                .id(cartItem.getId())
                                .productId(cartItem.getProductId())
                                .quantity(cartItem.getQuantity())
                                .price(cartItem.getPrice())
                                .subtotal(cartItem.getSubtotal())
                                .addedAt(cartItem.getAddedAt())

                                // ✅ FALLBACK PRODUCT DATA
                                .productName("Product not found")
                                .productImage(null)
                                .productStatus("UNKNOWN")
                                .inStock(false)
                                .availableQuantity(0)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        log.info("🔍 SERVICE: Successfully enriched {} items", enrichedItems.size());

        // ✅ PRESERVE ALL ORIGINAL CART DATA, only update items and timestamp
        EnrichedShoppingCartResponse.EnrichedCartResponseDTO result =
                EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                        // ✅ PRESERVE ALL ORIGINAL CART FIELDS
                        .id(cartResponse.getId())                           // Original cart ID
                        .userId(cartResponse.getUserId())                   // Original user ID
                        .total(cartResponse.getTotal())                     // Original total
                        .itemCount(cartResponse.getItemCount())             // Original item count
                        .totalQuantity(cartResponse.getTotalQuantity())     // Original total quantity
                        .createdAt(cartResponse.getCreatedAt())             // Original creation time
                        .expiresAt(cartResponse.getExpiresAt())             // Original expiration time

                        // ✅ UPDATE ONLY ENRICHED DATA
                        .items(enrichedItems)                               // Enriched items
                        .updatedAt(LocalDateTime.now())                     // Updated timestamp
                        .build();

        log.info("🔍 SERVICE: Final merged cart result - id: {}, userId: {}, total: {}, itemCount: {}, totalQuantity: {}",
                result.getId(), result.getUserId(), result.getTotal(),
                result.getItemCount(), result.getTotalQuantity());

        return result;
    }

    /**
     * ✅ OLD: Merge cart data with product details - preserving ALL cart data (keeping for reference)
     */
    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO mergeCartWithProducts(
            EnrichedShoppingCartResponse.EnrichedCartResponseDTO cartResponse,
            ProductBatchResponseDTO productBatchResponse) {

        log.info("🔍 SERVICE: Merging cart with {} cart items and {} product details",
                cartResponse.getItemCount(),
                productBatchResponse.getProducts() != null ? productBatchResponse.getProducts().size() : 0);

        // Create a map of productId -> product details for quick lookup
        Map<UUID, ProductBatchInfoDTO> productMap = productBatchResponse.getProducts().stream()
                .collect(Collectors.toMap(
                        ProductBatchInfoDTO::getId,
                        product -> product,
                        (existing, replacement) -> existing // Handle duplicates
                ));

        log.info("🔍 SERVICE: Created product map with {} entries", productMap.size());

        // Enrich cart items with product details
        List<EnrichedCartItemDTO> enrichedItems = cartResponse.getItems().stream()
                .map(cartItem -> {
                    ProductBatchInfoDTO productDetail = productMap.get(cartItem.getProductId());

                    if (productDetail != null) {
                        log.debug("🔍 SERVICE: Enriching cart item {} with product details", cartItem.getProductId());

                        // FIXED: Merge ALL cart item data with product details
                        return EnrichedCartItemDTO.builder()
                                // ✅ PRESERVE ALL CART-SPECIFIC DATA
                                .id(cartItem.getId())                           // Cart item ID
                                .productId(cartItem.getProductId())             // Product ID
                                .quantity(cartItem.getQuantity())               // Quantity in cart
                                .price(cartItem.getPrice())                     // Price from cart
                                .subtotal(cartItem.getSubtotal())               // Subtotal from cart
                                .addedAt(cartItem.getAddedAt())                 // When added to cart

                                // ✅ ADD PRODUCT DETAILS FROM PRODUCT SERVICE
                                .productName(productDetail.getName())           // Product name
                                .productImage(productDetail.getImagePath())    // Product image
                                .productStatus(productDetail.getStatus())      // Product status
                                .inStock(productDetail.getInStock())           // Stock status
                                .availableQuantity(productDetail.getAvailableQuantity()) // Available quantity
                                .build();
                    } else {
                        // Product details not found, keep cart item as is with fallback product info
                        log.warn("🔍 SERVICE: Product details not found for productId: {}", cartItem.getProductId());
                        return EnrichedCartItemDTO.builder()
                                // ✅ PRESERVE ALL CART DATA
                                .id(cartItem.getId())
                                .productId(cartItem.getProductId())
                                .quantity(cartItem.getQuantity())
                                .price(cartItem.getPrice())
                                .subtotal(cartItem.getSubtotal())
                                .addedAt(cartItem.getAddedAt())

                                // ✅ FALLBACK PRODUCT DATA
                                .productName("Product not found")
                                .productImage(null)
                                .productStatus("UNKNOWN")
                                .inStock(false)
                                .availableQuantity(0)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        log.info("🔍 SERVICE: Successfully enriched {} items", enrichedItems.size());

        // ✅ FIXED: PRESERVE ALL ORIGINAL CART DATA, only update items and timestamp
        EnrichedShoppingCartResponse.EnrichedCartResponseDTO result =
                EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                        // ✅ PRESERVE ALL ORIGINAL CART FIELDS
                        .id(cartResponse.getId())                           // Original cart ID
                        .userId(cartResponse.getUserId())                   // Original user ID
                        .total(cartResponse.getTotal())                     // Original total
                        .itemCount(cartResponse.getItemCount())             // Original item count
                        .totalQuantity(cartResponse.getTotalQuantity())     // Original total quantity
                        .createdAt(cartResponse.getCreatedAt())             // Original creation time
                        .expiresAt(cartResponse.getExpiresAt())             // Original expiration time

                        // ✅ UPDATE ONLY ENRICHED DATA
                        .items(enrichedItems)                               // Enriched items
                        .updatedAt(LocalDateTime.now())                     // Updated timestamp
                        .build();

        log.info("🔍 SERVICE: Final merged cart result - id: {}, userId: {}, total: {}, itemCount: {}, totalQuantity: {}",
                result.getId(), result.getUserId(), result.getTotal(),
                result.getItemCount(), result.getTotalQuantity());

        return result;
    }

    /**
     * ✅ Create empty cart response as fallback
     */
    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO createEmptyCartResponse(String userId) {
        log.warn("Creating empty cart response fallback for userId: {}", userId);

        UUID userUuid = null;
        try {
            if (userId != null && !userId.trim().isEmpty()) {
                userUuid = UUID.fromString(userId);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format for fallback: {}", userId);
        }

        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .id(null)
                .userId(userUuid)
                .items(List.of())
                .total(BigDecimal.ZERO)
                .itemCount(0)
                .totalQuantity(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();
    }

    /**
     * ✅ Get basic cart data without product enrichment (for internal use)
     */
    public Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> getBasicCart(String userId) {
        return getEnrichedCart(userId);
    }
}