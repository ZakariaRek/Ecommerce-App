package com.Ecommerce.Gateway_Service.Controllers;

import com.Ecommerce.Gateway_Service.DTOs.Order.BatchOrderRequestDTO;
import com.Ecommerce.Gateway_Service.DTOs.Order.BatchOrderResponseDTO;
import com.Ecommerce.Gateway_Service.DTOs.Order.EnrichedOrderResponse;
import com.Ecommerce.Gateway_Service.Service.AsyncOrderBffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AsyncEnrichedOrderBffController {

    private final AsyncOrderBffService asyncOrderBffService;

    /**
     * ✅ Get enriched order with product details
     */
    @GetMapping("/{orderId}/enriched")
    public Mono<ResponseEntity<EnrichedOrderResponse>> getEnrichedOrder(
            @PathVariable String orderId,
            @RequestParam(defaultValue = "true") boolean includeProducts) {

        log.info("🎯 CONTROLLER: Received request to get enriched order: {} with includeProducts: {}",
                orderId, includeProducts);

        if (includeProducts) {
            return asyncOrderBffService.getEnrichedOrderWithProducts(orderId)
                    .map(ResponseEntity::ok)
                    .doOnSuccess(response -> {
                        log.info("🎯 CONTROLLER: Successfully returned enriched order with products");
                    })
                    .onErrorResume(error -> {
                        log.error("🎯 CONTROLLER: Error getting enriched order", error);
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });
        } else {
            return asyncOrderBffService.getBasicOrder(orderId)
                    .map(ResponseEntity::ok)
                    .doOnSuccess(response -> {
                        log.info("🎯 CONTROLLER: Successfully returned basic order without products");
                    })
                    .onErrorResume(error -> {
                        log.error("🎯 CONTROLLER: Error getting basic order", error);
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });
        }
    }

    /**
     * ✅ Get order by user ID (enriched with products)
     */
    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<EnrichedOrderResponse>> getUserOrders(
            @PathVariable String userId,
            @RequestParam(required = false) String status) {

        log.info("🎯 CONTROLLER: Received request to get orders for user: {} with status: {}",
                userId, status);

        // This method would need to be implemented in the service
        // For now, returning a placeholder
        return Mono.just(ResponseEntity.ok(EnrichedOrderResponse.builder()
                .userId(java.util.UUID.fromString(userId))
                .status(status != null ? status : "ALL")
                .items(java.util.List.of())
                .build()));
    }


    /**
     * ✅ Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("Order BFF Service is healthy"));
    }

    /**
     * ✅ Get multiple enriched orders (batch processing) - with enhanced debugging
     */
    @GetMapping("/batch")
    public Mono<ResponseEntity<BatchOrderResponseDTO>> getEnrichedOrdersBatch(
            @RequestBody BatchOrderRequestDTO request) {

        log.info("🎯 CONTROLLER: === BATCH ENDPOINT HIT ===");
        log.info("🎯 CONTROLLER: Request received: {}", request);
        log.info("🎯 CONTROLLER: Order IDs count: {}", request.getOrderIds() != null ? request.getOrderIds().size() : 0);
        log.info("🎯 CONTROLLER: Order IDs: {}", request.getOrderIds());
        log.info("🎯 CONTROLLER: Include products: {}", request.isIncludeProducts());

        // Validate request
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            log.warn("🎯 CONTROLLER: Empty or null order IDs in batch request");
            BatchOrderResponseDTO emptyResponse = BatchOrderResponseDTO.builder()
                    .orders(List.of())
                    .failures(Map.of("validation", "No order IDs provided"))
                    .totalRequested(0)
                    .successful(0)
                    .failed(1)
                    .includeProducts(request.isIncludeProducts())
                    .processingTimeMs(0L)
                    .build();
            return Mono.just(ResponseEntity.badRequest().body(emptyResponse));
        }

        if (request.getOrderIds().size() > 50) { // Limit batch size
            log.warn("🎯 CONTROLLER: Batch size too large: {}", request.getOrderIds().size());
            BatchOrderResponseDTO errorResponse = BatchOrderResponseDTO.builder()
                    .orders(List.of())
                    .failures(Map.of("validation", "Batch size cannot exceed 50 orders"))
                    .totalRequested(request.getOrderIds().size())
                    .successful(0)
                    .failed(request.getOrderIds().size())
                    .includeProducts(request.isIncludeProducts())
                    .processingTimeMs(0L)
                    .build();
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        log.info("🎯 CONTROLLER: Calling asyncOrderBffService.getEnrichedOrdersBatch()");

        return asyncOrderBffService.getEnrichedOrdersBatchOptimized(request)
                .map(batchResponse -> {
                    log.info("🎯 CONTROLLER: === BATCH RESPONSE RECEIVED ===");
                    log.info("🎯 CONTROLLER: Batch response type: {}", batchResponse.getClass().getSimpleName());
                    log.info("🎯 CONTROLLER: Total requested: {}", batchResponse.getTotalRequested());
                    log.info("🎯 CONTROLLER: Successful: {}", batchResponse.getSuccessful());
                    log.info("🎯 CONTROLLER: Failed: {}", batchResponse.getFailed());
                    log.info("🎯 CONTROLLER: Processing time: {}ms", batchResponse.getProcessingTimeMs());
                    log.info("🎯 CONTROLLER: Orders count: {}", batchResponse.getOrders().size());
                    log.info("🎯 CONTROLLER: Failures: {}", batchResponse.getFailures());

                    return ResponseEntity.ok(batchResponse);
                })
                .doOnError(error -> {
                    log.error("🎯 CONTROLLER: === BATCH ERROR ===", error);
                })
                .onErrorResume(error -> {
                    log.error("🎯 CONTROLLER: Error processing batch order request", error);
                    BatchOrderResponseDTO errorResponse = BatchOrderResponseDTO.builder()
                            .orders(List.of())
                            .failures(Map.of("system_error", error.getMessage()))
                            .totalRequested(request.getOrderIds().size())
                            .successful(0)
                            .failed(request.getOrderIds().size())
                            .includeProducts(request.isIncludeProducts())
                            .processingTimeMs(0L)
                            .build();
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }


    @GetMapping("/user/{userId}/all")
    public Mono<ResponseEntity<BatchOrderResponseDTO>> getUserOrdersBatch(
            @PathVariable String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "true") boolean includeProducts,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("🎯 CONTROLLER: === USER ORDERS REQUEST ===");
        log.info("🎯 CONTROLLER: User ID: {}", userId);
        log.info("🎯 CONTROLLER: Status filter: {}", status);
        log.info("🎯 CONTROLLER: Include products: {}", includeProducts);
        log.info("🎯 CONTROLLER: Limit: {}", limit);

        // Validate and normalize userId (handle both UUID and ObjectId formats)
        if (userId == null || userId.trim().isEmpty()) {
            log.error("🎯 CONTROLLER: Invalid userId provided");
            BatchOrderResponseDTO errorResponse = BatchOrderResponseDTO.builder()
                    .orders(List.of())
                    .failures(Map.of("validation", "User ID is required"))
                    .totalRequested(0)
                    .successful(0)
                    .failed(1)
                    .includeProducts(includeProducts)
                    .processingTimeMs(0L)
                    .build();
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        // Parse and validate userId, convert to UUID
        UUID parsedUserId;
        try {
            parsedUserId = parseToUUID(userId);
            log.info("🎯 CONTROLLER: Successfully parsed userId to UUID: {}", parsedUserId);
        } catch (IllegalArgumentException e) {
            log.error("🎯 CONTROLLER: Invalid userId format: {}", userId);
            BatchOrderResponseDTO errorResponse = BatchOrderResponseDTO.builder()
                    .orders(List.of())
                    .failures(Map.of("validation", "Invalid User ID format"))
                    .totalRequested(0)
                    .successful(0)
                    .failed(1)
                    .includeProducts(includeProducts)
                    .processingTimeMs(0L)
                    .build();
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        return asyncOrderBffService.getUserOrderIds(parsedUserId, status, limit)
                .doOnNext(orderIds -> {
                    log.info("🎯 CONTROLLER: === ORDER IDS RECEIVED ===");
                    log.info("🎯 CONTROLLER: Received {} order IDs for user: {}", orderIds.size(), parsedUserId);
                    log.info("🎯 CONTROLLER: Order IDs: {}", orderIds);
                })
                .flatMap(orderIds -> {
                    if (orderIds.isEmpty()) {
                        log.info("🎯 CONTROLLER: No orders found for user: {}", parsedUserId);
                        BatchOrderResponseDTO emptyResponse = BatchOrderResponseDTO.builder()
                                .orders(List.of())
                                .failures(Map.of())
                                .totalRequested(0)
                                .successful(0)
                                .failed(0)
                                .includeProducts(includeProducts)
                                .processingTimeMs(0L)
                                .build();
                        return Mono.just(ResponseEntity.ok(emptyResponse));
                    }

                    log.info("🎯 CONTROLLER: === CREATING BATCH REQUEST ===");
                    log.info("🎯 CONTROLLER: Processing {} order IDs with includeProducts: {}",
                            orderIds.size(), includeProducts);

                    BatchOrderRequestDTO request = BatchOrderRequestDTO.builder()
                            .orderIds(orderIds)
                            .includeProducts(includeProducts)
                            .userId(parsedUserId.toString()) // Convert back to string for DTO
                            .status(status)
                            .build();

                    return asyncOrderBffService.getEnrichedOrdersBatchOptimized(request)
                            .doOnNext(batchResponse -> {
                                log.info("🎯 CONTROLLER: === BATCH RESPONSE RECEIVED ===");
                                log.info("🎯 CONTROLLER: Total requested: {}", batchResponse.getTotalRequested());
                                log.info("🎯 CONTROLLER: Successful: {}", batchResponse.getSuccessful());
                                log.info("🎯 CONTROLLER: Failed: {}", batchResponse.getFailed());
                                log.info("🎯 CONTROLLER: Processing time: {}ms", batchResponse.getProcessingTimeMs());
                                log.info("🎯 CONTROLLER: Include products: {}", batchResponse.isIncludeProducts());

                                // Log details about product enrichment
                                if (batchResponse.isIncludeProducts()) {
                                    batchResponse.getOrders().forEach(order -> {
                                        log.info("🎯 CONTROLLER: Order {} has {} items",
                                                order.getId(), order.getItems().size());
                                        order.getItems().forEach(item -> {
                                            log.info("🎯 CONTROLLER:   - Item: productId={}, name={}, inStock={}",
                                                    item.getProductId(), item.getProductName(), item.getInStock());
                                        });
                                    });
                                }
                            })
                            .map(ResponseEntity::ok);
                })
                .doOnError(error -> {
                    log.error("🎯 CONTROLLER: === ERROR IN USER ORDERS BATCH ===", error);
                    log.error("🎯 CONTROLLER: Error for user: {}, status: {}, includeProducts: {}",
                            parsedUserId, status, includeProducts);
                })
                .onErrorResume(error -> {
                    log.error("🎯 CONTROLLER: Error getting user orders batch", error);
                    BatchOrderResponseDTO errorResponse = BatchOrderResponseDTO.builder()
                            .orders(List.of())
                            .failures(Map.of("system_error", error.getMessage()))
                            .totalRequested(0)
                            .successful(0)
                            .failed(1)
                            .includeProducts(includeProducts)
                            .processingTimeMs(0L)
                            .build();
                    return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
                });
    }


    /**
     * Converts MongoDB ObjectId to UUID using deterministic MD5 hashing
     */
    private UUID convertObjectIdToUuid(String objectId) {
        if (objectId == null || !objectId.matches("^[a-fA-F0-9]{24}$")) {
            throw new IllegalArgumentException("Invalid ObjectId format: " + objectId);
        }

        try {
            // Use MD5 hash for deterministic conversion
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(objectId.getBytes(StandardCharsets.UTF_8));

            // Convert to UUID using the hash bytes
            long mostSigBits = 0;
            long leastSigBits = 0;

            for (int i = 0; i < 8; i++) {
                mostSigBits = (mostSigBits << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                leastSigBits = (leastSigBits << 8) | (hash[i] & 0xff);
            }

            UUID result = new UUID(mostSigBits, leastSigBits);
            log.info("🎯 Converted ObjectId {} to UUID {}", objectId, result);

            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Alternative: Simple deterministic conversion using ObjectId structure
     */
    private UUID convertObjectIdToUuidSimple(String objectId) {
        if (objectId == null || !objectId.matches("^[a-fA-F0-9]{24}$")) {
            throw new IllegalArgumentException("Invalid ObjectId format: " + objectId);
        }

        // Take the first 12 bytes (24 hex chars) of ObjectId
        // ObjectId structure: 4-byte timestamp + 5-byte random + 3-byte counter
        String timestamp = objectId.substring(0, 8);    // First 4 bytes
        String machine = objectId.substring(8, 18);     // Next 5 bytes
        String counter = objectId.substring(18, 24);    // Last 3 bytes

        // Pad to create 32 hex characters for UUID
        String uuidHex = timestamp + "0000" + machine + counter + "00";

        // Parse as UUID
        long mostSigBits = Long.parseUnsignedLong(uuidHex.substring(0, 16), 16);
        long leastSigBits = Long.parseUnsignedLong(uuidHex.substring(16, 32), 16);

        UUID result = new UUID(mostSigBits, leastSigBits);
        log.info("🎯 Converted ObjectId {} to UUID {}", objectId, result);

        return result;
    }

    /**
     * Utility method to handle both UUID and ObjectId formats
     */
    private UUID parseToUUID(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        userId = userId.trim();

        try {
            // Try to parse as UUID first
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // If not a UUID, check if it's an ObjectId
            if (userId.matches("^[a-fA-F0-9]{24}$")) {
                return convertObjectIdToUuid(userId);
            } else {
                throw new IllegalArgumentException("Invalid User ID format: " + userId +
                        ". Must be either UUID or MongoDB ObjectId (24 hex characters)");
            }
        }
    }
}