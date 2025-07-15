package com.Ecommerce.Gateway_Service.Controllers;

import com.Ecommerce.Gateway_Service.DTOs.Order.EnrichedOrderResponse;
import com.Ecommerce.Gateway_Service.Service.AsyncOrderBffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
     * ✅ Health check endpoint
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("Order BFF Service is healthy"));
    }
}