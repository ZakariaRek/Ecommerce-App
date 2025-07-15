// Gateway-Service/src/main/java/com/Ecommerce/Gateway_Service/Service/AsyncSaved4LaterBffService.java
package com.Ecommerce.Gateway_Service.Service;

import com.Ecommerce.Gateway_Service.DTOs.Cart.EnrichedCartItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.EnrichedSavedItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.EnrichedSavedItemsResponse;
import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.SavedItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.SavedItemsResponseDTO;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.Ecommerce.Gateway_Service.Kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncSaved4LaterBffService {

    private final KafkaTemplate<String, Object> gatewayKafkaTemplate;
    private final AsyncResponseManager asyncResponseManager;
    private final AsyncProductService asyncProductService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Get saved items using async Kafka communication - WITHOUT product enrichment
     */
    public Mono<SavedItemsResponseDTO> getSavedItems(String userId) {
        String correlationId = UUID.randomUUID().toString();

        log.info("Starting async saved4later request for userId: {} with correlationId: {}", userId, correlationId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                log.error("Invalid userId provided: {}", userId);
                return Mono.just(createEmptyResponse(userId));
            }

            // Create saved4later request
            Map<String, Object> saved4laterRequest = new HashMap<>();
            saved4laterRequest.put("correlationId", correlationId);
            saved4laterRequest.put("userId", userId);
            saved4laterRequest.put("timestamp", System.currentTimeMillis());

            log.info("🔍 SERVICE: Sending saved4later request to Kafka: {}", saved4laterRequest);

            // Send request to cart service (saved4later endpoints)
            gatewayKafkaTemplate.send(KafkaTopics.SAVED4LATER_REQUEST, correlationId, saved4laterRequest);

            // Wait for response with timeout
            Duration timeout = Duration.ofSeconds(30);

            return asyncResponseManager.waitForResponse(
                            correlationId,
                            timeout,
                            SavedItemsResponseDTO.class
                    )
                    .doOnSuccess(response -> {
                        log.info("🔍 SERVICE: Successfully received async saved4later response for correlationId: {} with {} items",
                                correlationId, response.getItemCount());
                        log.info("🔍 SERVICE: Saved4Later response details - userId: {}, itemCount: {}, lastUpdated: {}",
                                response.getUserId(), response.getItemCount(), response.getLastUpdated());
                    })
                    .doOnError(error -> {
                        log.error("Failed to get async saved4later response for correlationId: {}", correlationId, error);
                    })
                    .onErrorReturn(createEmptyResponse(userId));

        } catch (Exception e) {
            log.error("Error initiating async saved4later request for userId: {}", userId, e);
            return Mono.just(createEmptyResponse(userId));
        }
    }

    /**
     * ✅ Get enriched saved items WITH product enrichment
     */
    public Mono<EnrichedSavedItemsResponse> getEnrichedSavedItemsWithProducts(String userId) {
        log.info("🔍 SERVICE: Starting enriched saved4later with products for userId: {}", userId);

        return getSavedItems(userId)
                .flatMap(savedItemsResponse -> {
                    log.info("🔍 SERVICE: Received saved4later response before product enrichment:");
                    log.info("   - User ID: {}", savedItemsResponse.getUserId());
                    log.info("   - Item Count: {}", savedItemsResponse.getItemCount());
                    log.info("   - Last Updated: {}", savedItemsResponse.getLastUpdated());

                    if (savedItemsResponse.getItemCount() != null && savedItemsResponse.getItemCount() > 0) {
                        // Extract product IDs from saved items
                        List<UUID> productIds = savedItemsResponse.getItems().stream()
                                .map(SavedItemDTO::getProductId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        if (!productIds.isEmpty()) {
                            log.info("🔍 SERVICE: Enriching {} saved items with product data", productIds.size());
                            log.info("🔍 SERVICE: Product IDs to fetch: {}", productIds);

                            return asyncProductService.getProductsBatch(productIds)
                                    .map(productResponse -> {
                                        log.info("🔍 SERVICE: Received product response");
                                        log.info("🔍 SERVICE: Response type: {}", productResponse.getClass().getSimpleName());

                                        @SuppressWarnings("unchecked")
                                        List<EnrichedCartItemDTO> productItems = (List<EnrichedCartItemDTO>) productResponse;

                                        log.info("🔍 SERVICE: Successfully cast to List<EnrichedCartItemDTO> with {} items", productItems.size());

                                        // Log the product details we received
                                        for (EnrichedCartItemDTO product : productItems) {
                                            log.info("🔍 SERVICE: Product: id={}, name={}, status={}, inStock={}, availableQuantity={}",
                                                    product.getProductId(), product.getProductName(),
                                                    product.getProductStatus(), product.getInStock(), product.getAvailableQuantity());
                                        }

                                        return mergeSavedItemsWithProductItems(savedItemsResponse, productItems);
                                    })
                                    .doOnSuccess(enrichedSavedItems -> {
                                        log.info("🔍 SERVICE: Successfully enriched saved4later with product details");
                                        log.info("🔍 SERVICE: Final enriched saved4later - userId: {}, itemCount: {}, availableItems: {}, unavailableItems: {}",
                                                enrichedSavedItems.getUserId(), enrichedSavedItems.getItemCount(),
                                                enrichedSavedItems.getAvailableItemsCount(), enrichedSavedItems.getUnavailableItemsCount());
                                    })
                                    .onErrorResume(error -> {
                                        log.error("🔍 SERVICE: Error enriching saved4later with products", error);
                                        return Mono.just(createEnrichedResponseFromBasic(savedItemsResponse)); // Return without enrichment on error
                                    });
                        }
                    }

                    log.info("🔍 SERVICE: Saved4Later has no items to enrich, returning basic data");
                    return Mono.just(createEnrichedResponseFromBasic(savedItemsResponse));
                });
    }

    /**
     * ✅ Enhanced merge method for saved items with product details
     */
    private EnrichedSavedItemsResponse mergeSavedItemsWithProductItems(
            SavedItemsResponseDTO savedItemsResponse,
            List<EnrichedCartItemDTO> productItems) {

        log.info("🔍 SERVICE: Merging saved4later with {} saved items and {} product items",
                savedItemsResponse.getItemCount(), productItems.size());

        // Create a map of productId -> product details for quick lookup
        Map<UUID, EnrichedCartItemDTO> productMap = productItems.stream()
                .collect(Collectors.toMap(
                        EnrichedCartItemDTO::getProductId,
                        product -> product,
                        (existing, replacement) -> existing // Handle duplicates
                ));

        log.info("🔍 SERVICE: Created product map with {} entries", productMap.size());

        // Enrich saved items with product details
        List<EnrichedSavedItemDTO> enrichedItems = savedItemsResponse.getItems().stream()
                .map(savedItem -> {
                    EnrichedCartItemDTO productDetail = productMap.get(savedItem.getProductId());

                    if (productDetail != null) {
                        log.info("🔍 SERVICE: Enriching saved item {} with product details", savedItem.getProductId());

                        return EnrichedSavedItemDTO.builder()
                                // Preserve all saved item data
                                .id(savedItem.getId())
                                .userId(savedItem.getUserId())
                                .productId(savedItem.getProductId())
                                .savedAt(savedItem.getSavedAt())

                                // Add product details from product service
                                .productName(productDetail.getProductName())
                                .productImage(productDetail.getProductImage())
                                .productStatus(productDetail.getProductStatus())
                                .inStock(productDetail.getInStock())
                                .availableQuantity(productDetail.getAvailableQuantity())
                                .price(productDetail.getPrice())
                                .discountValue(productDetail.getDiscountValue())
                                .discountType(productDetail.getDiscountType())
                                .build();
                    } else {
                        // Product details not found, keep saved item as is with fallback product info
                        log.warn("🔍 SERVICE: Product details not found for productId: {}", savedItem.getProductId());
                        return EnrichedSavedItemDTO.builder()
                                // Preserve all saved item data
                                .id(savedItem.getId())
                                .userId(savedItem.getUserId())
                                .productId(savedItem.getProductId())
                                .savedAt(savedItem.getSavedAt())

                                // Fallback product data
                                .productName("Product not found")
                                .productImage(null)
                                .productStatus("UNKNOWN")
                                .inStock(false)
                                .availableQuantity(0)
                                .price(null)
                                .discountValue(null)
                                .discountType(null)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        log.info("🔍 SERVICE: Successfully enriched {} items", enrichedItems.size());

        // Calculate availability statistics
        int availableItemsCount = (int) enrichedItems.stream()
                .filter(item -> item.getInStock() != null && item.getInStock())
                .count();
        int unavailableItemsCount = enrichedItems.size() - availableItemsCount;

        // Build the enriched response
        EnrichedSavedItemsResponse result = EnrichedSavedItemsResponse.builder()
                .userId(savedItemsResponse.getUserId())
                .items(enrichedItems)
                .itemCount(enrichedItems.size())
                .lastUpdated(LocalDateTime.now())
                .availableItemsCount(availableItemsCount)
                .unavailableItemsCount(unavailableItemsCount)
                .build();

        log.info("🔍 SERVICE: Final merged saved4later result - userId: {}, itemCount: {}, availableItems: {}, unavailableItems: {}",
                result.getUserId(), result.getItemCount(), result.getAvailableItemsCount(), result.getUnavailableItemsCount());

        return result;
    }

    /**
     * ✅ Create enriched response from basic response (when no products to enrich)
     */
    private EnrichedSavedItemsResponse createEnrichedResponseFromBasic(SavedItemsResponseDTO basicResponse) {
        // Convert basic saved items to enriched items without product data
        List<EnrichedSavedItemDTO> enrichedItems = basicResponse.getItems().stream()
                .map(savedItem -> EnrichedSavedItemDTO.builder()
                        .id(savedItem.getId())
                        .userId(savedItem.getUserId())
                        .productId(savedItem.getProductId())
                        .savedAt(savedItem.getSavedAt())
                        // Default product data
                        .productName("Loading...")
                        .productImage(null)
                        .productStatus("UNKNOWN")
                        .inStock(null)
                        .availableQuantity(null)
                        .price(null)
                        .discountValue(null)
                        .discountType(null)
                        .build())
                .collect(Collectors.toList());

        return EnrichedSavedItemsResponse.builder()
                .userId(basicResponse.getUserId())
                .items(enrichedItems)
                .itemCount(basicResponse.getItemCount())
                .lastUpdated(basicResponse.getLastUpdated())
                .availableItemsCount(0)
                .unavailableItemsCount(0)
                .build();
    }

    /**
     * ✅ Create empty response as fallback
     */
    private SavedItemsResponseDTO createEmptyResponse(String userId) {
        log.warn("Creating empty saved4later response fallback for userId: {}", userId);

        UUID userUuid = null;
        try {
            if (userId != null && !userId.trim().isEmpty()) {
                userUuid = UUID.fromString(userId);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format for fallback: {}", userId);
        }

        return SavedItemsResponseDTO.builder()
                .userId(userUuid)
                .items(List.of())
                .itemCount(0)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * ✅ Get basic saved items data without product enrichment (for internal use)
     */
    public Mono<SavedItemsResponseDTO> getBasicSavedItems(String userId) {
        return getSavedItems(userId);
    }
}