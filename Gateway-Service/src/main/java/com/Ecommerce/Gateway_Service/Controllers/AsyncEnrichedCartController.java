package com.Ecommerce.Gateway_Service.Controllers;

import com.Ecommerce.Gateway_Service.DTOs.EnrichedShoppingCartResponse;
import com.Ecommerce.Gateway_Service.Service.AsyncCartBffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "BFF Cart", description = "Backend for Frontend Cart operations with enriched product data")
@RequiredArgsConstructor
@Slf4j
public class AsyncEnrichedCartController {

    private final AsyncCartBffService asyncCartBffService; // 🔄 Use async service

    @Operation(
            summary = "Get enriched cart data (Async)",
            description = "Returns cart data enriched with product information using async Kafka communication"
    )
    @GetMapping("/{userId}/enriched")
    public Mono<ResponseEntity<EnrichedShoppingCartResponse.EnrichedCartResponseDTO>> getEnrichedCart(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId) {

        log.info("Fetching enriched cart for user: {} (async)", userId);

        return asyncCartBffService.getEnrichedCart(userId) // 🔄 Now async
                .map(enrichedCart -> {
                    log.info("Successfully fetched enriched cart for user: {} with {} items",
                            userId, enrichedCart.getItemCount());
                    return ResponseEntity.ok(enrichedCart);
                })
                .onErrorResume(error -> {
                    log.error("Error fetching enriched cart for user {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}