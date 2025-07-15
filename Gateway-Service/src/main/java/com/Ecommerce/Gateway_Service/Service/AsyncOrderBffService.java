package com.Ecommerce.Gateway_Service.Service;

import com.Ecommerce.Gateway_Service.DTOs.Cart.EnrichedCartItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.EnrichedOrderItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.Order.BatchOrderRequestDTO;
import com.Ecommerce.Gateway_Service.DTOs.Order.BatchOrderResponseDTO;
import com.Ecommerce.Gateway_Service.DTOs.Order.EnrichedOrderResponse;
import com.Ecommerce.Gateway_Service.DTOs.Order.OrderResult;
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
public class AsyncOrderBffService {

    private final KafkaTemplate<String, Object> gatewayKafkaTemplate;
    private final AsyncResponseManager asyncResponseManager;
    private final AsyncProductService asyncProductService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ Get enriched order using async Kafka communication - WITHOUT product enrichment
     */
    public Mono<EnrichedOrderResponse> getEnrichedOrder(String orderId) {
        String correlationId = UUID.randomUUID().toString();

        log.info("Starting async order request for orderId: {} with correlationId: {}", orderId, correlationId);

        try {
            if (orderId == null || orderId.trim().isEmpty()) {
                log.error("Invalid orderId provided: {}", orderId);
                return Mono.just(createEmptyOrderResponse());
            }

            // Create order request
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("correlationId", correlationId);
            orderRequest.put("orderId", orderId);
            orderRequest.put("timestamp", System.currentTimeMillis());

            log.info("🔍 SERVICE: Sending order request to Kafka: {}", orderRequest);

            // Send request to order service
            gatewayKafkaTemplate.send("order.request", correlationId, orderRequest);

            // Wait for response with timeout
            Duration timeout = Duration.ofSeconds(30);

            return asyncResponseManager.waitForResponse(
                            correlationId,
                            timeout,
                            EnrichedOrderResponse.class
                    )
                    .doOnSuccess(response -> {
                        log.info("🔍 SERVICE: Successfully received async order response for correlationId: {} with {} items",
                                correlationId, response.getItems().size());
                        log.info("🔍 SERVICE: Order response details - id: {}, userId: {}, status: {}, totalAmount: {}",
                                response.getId(), response.getUserId(), response.getStatus(), response.getTotalAmount());
                    })
                    .doOnError(error -> {
                        log.error("Failed to get async order response for correlationId: {}", correlationId, error);
                    })
                    .onErrorReturn(createEmptyOrderResponse());

        } catch (Exception e) {
            log.error("Error initiating async order request for orderId: {}", orderId, e);
            return Mono.just(createEmptyOrderResponse());
        }
    }

    /**
     * ✅ Get enriched order WITH product enrichment
     */
    public Mono<EnrichedOrderResponse> getEnrichedOrderWithProducts(String orderId) {
        log.info("🔍 SERVICE: Starting enriched order with products for orderId: {}", orderId);

        return getEnrichedOrder(orderId)
                .flatMap(orderResponse -> {
                    log.info("🔍 SERVICE: Received order response before product enrichment:");
                    log.info("   - Order ID: {}", orderResponse.getId());
                    log.info("   - User ID: {}", orderResponse.getUserId());
                    log.info("   - Status: {}", orderResponse.getStatus());
                    log.info("   - Total Amount: {}", orderResponse.getTotalAmount());
                    log.info("   - Item Count: {}", orderResponse.getItems().size());

                    if (!orderResponse.getItems().isEmpty()) {
                        // Extract product IDs from order items
                        List<UUID> productIds = orderResponse.getItems().stream()
                                .map(EnrichedOrderItemDTO::getProductId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        if (!productIds.isEmpty()) {
                            log.info("🔍 SERVICE: Enriching {} order items with product data", productIds.size());
                            log.info("🔍 SERVICE: Product IDs to fetch: {}", productIds);

                            // Use the existing AsyncProductService to get product details
                            return asyncProductService.getProductsBatch(productIds)
                                    .map(productResponse -> {
                                        log.info("🔍 SERVICE: Received product response");
                                        log.info("🔍 SERVICE: Response type: {}", productResponse.getClass().getSimpleName());

                                        // AsyncProductService returns List<EnrichedCartItemDTO>
                                        List<EnrichedCartItemDTO> productItems = productResponse;

                                        log.info("🔍 SERVICE: Successfully received {} product items", productItems.size());

                                        // Log the product details we received
                                        for (EnrichedCartItemDTO product : productItems) {
                                            log.info("🔍 SERVICE: Product: id={}, name={}, status={}, inStock={}, availableQuantity={}",
                                                    product.getProductId(), product.getProductName(),
                                                    product.getProductStatus(), product.getInStock(), product.getAvailableQuantity());
                                        }

                                        // Merge order items with product details
                                        return mergeOrderWithProductItems(orderResponse, productItems);
                                    })
                                    .doOnSuccess(enrichedOrder -> {
                                        log.info("🔍 SERVICE: Successfully enriched order with product details");
                                        log.info("🔍 SERVICE: Final enriched order - id: {}, userId: {}, status: {}, totalAmount: {}",
                                                enrichedOrder.getId(), enrichedOrder.getUserId(),
                                                enrichedOrder.getStatus(), enrichedOrder.getTotalAmount());
                                    })
                                    .onErrorResume(error -> {
                                        log.error("🔍 SERVICE: Error enriching order with products", error);
                                        return Mono.just(orderResponse); // Return order without enrichment on error
                                    });
                        }
                    }

                    log.info("🔍 SERVICE: Order has no items to enrich, returning basic order data");
                    return Mono.just(orderResponse);
                });
    }

    /**
     * ✅ Merge order with product details
     */
    private EnrichedOrderResponse mergeOrderWithProductItems(
            EnrichedOrderResponse orderResponse,
            List<EnrichedCartItemDTO> productItems) {

        log.info("🔍 SERVICE: Merging order with {} order items and {} product items",
                orderResponse.getItems().size(), productItems.size());

        // Create a map of productId -> product details for quick lookup
        Map<UUID, EnrichedCartItemDTO> productMap = productItems.stream()
                .collect(Collectors.toMap(
                        EnrichedCartItemDTO::getProductId,
                        product -> product,
                        (existing, replacement) -> existing // Handle duplicates
                ));

        log.info("🔍 SERVICE: Created product map with {} entries", productMap.size());

        // Enrich order items with product details
        List<EnrichedOrderItemDTO> enrichedItems = orderResponse.getItems().stream()
                .map(orderItem -> {
                    EnrichedCartItemDTO productDetail = productMap.get(orderItem.getProductId());

                    if (productDetail != null) {
                        log.info("🔍 SERVICE: Enriching order item {} with product details", orderItem.getProductId());

                        // Merge order item data with product details
                        EnrichedOrderItemDTO enrichedItem = EnrichedOrderItemDTO.builder()
                                // Preserve all order-specific data
                                .id(orderItem.getId())
                                .productId(orderItem.getProductId())
                                .quantity(orderItem.getQuantity())
                                .priceAtPurchase(orderItem.getPriceAtPurchase())
                                .discount(orderItem.getDiscount())
                                .total(orderItem.getTotal())

                                // Add product details from product service
                                .productName(productDetail.getProductName())
                                .productImage(productDetail.getProductImage())
                                .productStatus(productDetail.getProductStatus())
                                .inStock(productDetail.getInStock())
                                .availableQuantity(productDetail.getAvailableQuantity())
                                .discountType(productDetail.getDiscountType())
                                .discountValue(productDetail.getDiscountValue())
                                .build();

                        log.info("🔍 SERVICE: Enriched order item - productId={}, name={}, quantity={}, total={}",
                                enrichedItem.getProductId(), enrichedItem.getProductName(),
                                enrichedItem.getQuantity(), enrichedItem.getTotal());

                        return enrichedItem;
                    } else {
                        // Product details not found, keep order item as is with fallback product info
                        log.warn("🔍 SERVICE: Product details not found for productId: {}", orderItem.getProductId());
                        return EnrichedOrderItemDTO.builder()
                                // Preserve all order data
                                .id(orderItem.getId())
                                .productId(orderItem.getProductId())
                                .quantity(orderItem.getQuantity())
                                .priceAtPurchase(orderItem.getPriceAtPurchase())
                                .discount(orderItem.getDiscount())
                                .total(orderItem.getTotal())

                                // Fallback product data
                                .productName("Product not found")
                                .productImage(null)
                                .productStatus("UNKNOWN")
                                .inStock(false)
                                .availableQuantity(0)
                                .discountValue(null)
                                .discountType(null)
                                .build();
                    }
                })
                .collect(Collectors.toList());

        log.info("🔍 SERVICE: Successfully enriched {} items", enrichedItems.size());

        // Preserve all original order data, only update items and timestamp
        EnrichedOrderResponse result = EnrichedOrderResponse.builder()
                // Preserve all original order fields
                .id(orderResponse.getId())
                .userId(orderResponse.getUserId())
                .cartId(orderResponse.getCartId())
                .status(orderResponse.getStatus())
                .totalAmount(orderResponse.getTotalAmount())
                .tax(orderResponse.getTax())
                .shippingCost(orderResponse.getShippingCost())
                .discount(orderResponse.getDiscount())
                .createdAt(orderResponse.getCreatedAt())
                .billingAddressId(orderResponse.getBillingAddressId())
                .shippingAddressId(orderResponse.getShippingAddressId())

                // Update only enriched data
                .items(enrichedItems)
                .updatedAt(LocalDateTime.now())
                .build();

        log.info("🔍 SERVICE: Final merged order result - id: {}, userId: {}, status: {}, totalAmount: {}, itemCount: {}",
                result.getId(), result.getUserId(), result.getStatus(),
                result.getTotalAmount(), result.getItems().size());

        return result;
    }

    /**
     * ✅ Create empty order response as fallback
     */
    private EnrichedOrderResponse createEmptyOrderResponse() {
        log.warn("Creating empty order response fallback");

        return EnrichedOrderResponse.builder()
                .id(null)
                .userId(null)
                .cartId(null)
                .status("UNKNOWN")
                .items(List.of())
                .totalAmount(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .shippingCost(BigDecimal.ZERO)
                .discount(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .billingAddressId(null)
                .shippingAddressId(null)
                .build();
    }

    /**
     * ✅ Get basic order data without product enrichment (for internal use)
     */
    public Mono<EnrichedOrderResponse> getBasicOrder(String orderId) {
        return getEnrichedOrder(orderId);
    }



    /**
     * ✅ Get multiple enriched orders using async Kafka communication
     */
    public Mono<BatchOrderResponseDTO> getEnrichedOrdersBatch(BatchOrderRequestDTO request) {
        log.info("🔍 SERVICE: Starting batch order request for {} orders with includeProducts: {}",
                request.getOrderIds().size(), request.isIncludeProducts());

        long startTime = System.currentTimeMillis();

        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            return Mono.just(createEmptyBatchResponse(request, startTime));
        }

        // Process orders in parallel
        List<Mono<OrderResult>> orderMonos = request.getOrderIds().stream()
                .map(orderId -> processOrderForBatch(orderId, request.isIncludeProducts()))
                .collect(Collectors.toList());

        // Combine all results
        return Mono.zip(orderMonos, results -> {
            List<EnrichedOrderResponse> successfulOrders = new ArrayList<>();
            Map<String, String> failures = new HashMap<>();

            for (Object result : results) {
                OrderResult orderResult = (OrderResult) result;
                if (orderResult.isSuccess()) {
                    successfulOrders.add(orderResult.getOrder());
                } else {
                    failures.put(orderResult.getOrderId(), orderResult.getErrorMessage());
                }
            }

            long processingTime = System.currentTimeMillis() - startTime;

            return BatchOrderResponseDTO.builder()
                    .orders(successfulOrders)
                    .failures(failures)
                    .totalRequested(request.getOrderIds().size())
                    .successful(successfulOrders.size())
                    .failed(failures.size())
                    .includeProducts(request.isIncludeProducts())
                    .processingTimeMs(processingTime)
                    .build();
        });
    }

    /**
     * ✅ Process individual order for batch operation
     */
    private Mono<OrderResult> processOrderForBatch(String orderId, boolean includeProducts) {
        Mono<EnrichedOrderResponse> orderMono = includeProducts
                ? getEnrichedOrderWithProducts(orderId)
                : getBasicOrder(orderId);

        return orderMono
                .map(order -> OrderResult.success(orderId, order))
                .onErrorResume(error -> {
                    log.warn("Failed to process order {} in batch: {}", orderId, error.getMessage());
                    return Mono.just(OrderResult.failure(orderId, error.getMessage()));
                });
    }

    /**
     * ✅ Create empty batch response
     */
    private BatchOrderResponseDTO createEmptyBatchResponse(BatchOrderRequestDTO request, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;

        return BatchOrderResponseDTO.builder()
                .orders(List.of())
                .failures(Map.of())
                .totalRequested(request.getOrderIds() != null ? request.getOrderIds().size() : 0)
                .successful(0)
                .failed(0)
                .includeProducts(request.isIncludeProducts())
                .processingTimeMs(processingTime)
                .build();
    }

}