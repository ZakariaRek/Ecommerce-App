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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
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
     * ✅ Get orders by multiple order IDs (GET version for simple batch)
     */
    @PostMapping("/batch")
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

        return asyncOrderBffService.getEnrichedOrdersBatch(request)
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

    /**
     * ✅ Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("Order BFF Service is healthy"));
    }


}