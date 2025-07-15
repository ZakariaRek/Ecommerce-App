package com.Ecommerce.Gateway_Service.Service;

import com.Ecommerce.Gateway_Service.DTOs.Cart.EnrichedCartItemDTO;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.ProductBatchRequestEventDTO;
import com.Ecommerce.Gateway_Service.Kafka.DTOs.ProductBatchResponseDTO;
import com.Ecommerce.Gateway_Service.DTOs.Product.ProductBatchInfoDTO;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.Ecommerce.Gateway_Service.Kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncProductService {

    private final KafkaTemplate<String, Object> gatewayKafkaTemplate;
    private final AsyncResponseManager asyncResponseManager;

    /**
     * ✅ Fetch product details for multiple product IDs using async Kafka communication
     */
    public Mono<List<EnrichedCartItemDTO>> getProductsBatch(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            log.warn("No product IDs provided for batch request");
            return Mono.just(List.of());
        }

        String correlationId = UUID.randomUUID().toString();

        log.info("Starting async product batch request for {} products with correlationId: {}",
                productIds.size(), correlationId);

        try {
            // Create proper DTO request
            ProductBatchRequestEventDTO productRequest = ProductBatchRequestEventDTO.builder()
                    .correlationId(correlationId)
                    .productIds(productIds)
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("Sending product batch request to Kafka topic '{}': {}",
                    KafkaTopics.PRODUCT_BATCH_REQUEST, productRequest);

            // Send request to product service
            gatewayKafkaTemplate.send(KafkaTopics.PRODUCT_BATCH_REQUEST, correlationId, productRequest)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send product request to Kafka", ex);
                        } else {
                            log.info("Successfully sent product request to Kafka: {}", result.getRecordMetadata());
                        }
                    });

            // Wait for response with timeout
            Duration timeout = Duration.ofSeconds(30);

            return asyncResponseManager.waitForResponse(  // ✅ Updated method name
                            correlationId,
                            timeout,
                            ProductBatchResponseDTO.class
                    )
                    .map(this::convertToEnrichedCartItems)
                    .doOnSuccess(response -> {
                        log.info("Successfully received async product batch response for correlationId: {} with {} products",
                                correlationId, response.size());
                    })
                    .doOnError(error -> {
                        log.error("Failed to get async product batch response for correlationId: {}",
                                correlationId, error);
                    })
                    .onErrorReturn(List.of()); // ✅ Return empty list on error

        } catch (Exception e) {
            log.error("Error initiating async product batch request for products: {}", productIds, e);
            return Mono.just(List.of());
        }
    }

    /**
     * ✅ Convert ProductBatchResponseDTO to List<EnrichedCartItemDTO>
     */
    private List<EnrichedCartItemDTO> convertToEnrichedCartItems(ProductBatchResponseDTO response) {
        if (response == null || !response.isSuccess() || response.getProducts() == null) {
            log.warn("Invalid or unsuccessful product batch response: {}", response);
            return List.of();
        }

        return response.getProducts().stream()
                .map(this::convertToEnrichedCartItem)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Convert single ProductBatchInfoDTO to EnrichedCartItemDTO
     */
    private EnrichedCartItemDTO convertToEnrichedCartItem(ProductBatchInfoDTO productInfo) {
        return EnrichedCartItemDTO.builder()
                .productId(productInfo.getId())
                .productName(productInfo.getName())
                .productImage(productInfo.getImagePath())
                .price(productInfo.getPrice())
                .availableQuantity(productInfo.getAvailableQuantity())
                .inStock(productInfo.getInStock())
                .discountType(productInfo.getDiscountType())
                .discountValue(productInfo.getDiscountValue())
                .productStatus(productInfo.getStatus() != null ? productInfo.getStatus().toString() : null)
                .build();
    }


}