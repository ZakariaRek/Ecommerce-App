// Gateway-Service/src/main/java/com/Ecommerce/Gateway_Service/Controllers/AsyncEnrichedSaved4LaterController.java
package com.Ecommerce.Gateway_Service.Controllers;

import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.EnrichedSavedItemsResponse;
import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.SavedItemsResponseDTO;
import com.Ecommerce.Gateway_Service.Service.AsyncSaved4LaterBffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/saved4later")
@Tag(name = "BFF Save4Later", description = "Backend for Frontend Save4Later operations with enriched product data")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AsyncEnrichedSaved4LaterController {

    private final AsyncSaved4LaterBffService asyncSaved4LaterBffService;

    @Operation(
            summary = "Get enriched saved items with product details (Async)",
            description = "Returns saved for later items enriched with complete product information using async Kafka communication"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved enriched saved items",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EnrichedSavedItemsResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}/enriched")
    public Mono<ResponseEntity<EnrichedSavedItemsResponse>> getEnrichedSavedItems(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {

        log.info("Fetching enriched saved items with products for user: {} (async)", userId);

        return asyncSaved4LaterBffService.getEnrichedSavedItemsWithProducts(userId)
                .map(enrichedSavedItems -> {
                    log.info("Successfully fetched enriched saved items for user: {} with {} items ({} available, {} unavailable)",
                            userId, enrichedSavedItems.getItemCount(),
                            enrichedSavedItems.getAvailableItemsCount(),
                            enrichedSavedItems.getUnavailableItemsCount());
                    return ResponseEntity.ok(enrichedSavedItems);
                })
                .onErrorResume(error -> {
                    log.error("Error fetching enriched saved items for user {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Operation(
            summary = "Get basic saved items data (Async)",
            description = "Returns basic saved for later items without product enrichment using async Kafka communication"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved basic saved items",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SavedItemsResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}/basic")
    public Mono<ResponseEntity<SavedItemsResponseDTO>> getBasicSavedItems(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {

        log.info("Fetching basic saved items for user: {} (async)", userId);

        return asyncSaved4LaterBffService.getBasicSavedItems(userId)
                .map(savedItems -> {
                    log.info("Successfully fetched basic saved items for user: {} with {} items",
                            userId, savedItems.getItemCount());
                    return ResponseEntity.ok(savedItems);
                })
                .onErrorResume(error -> {
                    log.error("Error fetching basic saved items for user {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Operation(
            summary = "Get saved items availability summary",
            description = "Returns summary of saved items availability (in stock vs out of stock count)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved availability summary",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}/availability-summary")
    public Object getSavedItemsAvailabilitySummary(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {

        log.info("Fetching saved items availability summary for user: {}", userId);

        return asyncSaved4LaterBffService.getEnrichedSavedItemsWithProducts(userId)
                .map(enrichedSavedItems -> {
                    var summary = new Object() {
                        public final String userId = enrichedSavedItems.getUserId().toString();
                        public final int totalItems = enrichedSavedItems.getItemCount();
                        public final int availableItems = enrichedSavedItems.getAvailableItemsCount();
                        public final int unavailableItems = enrichedSavedItems.getUnavailableItemsCount();
                        public final double availabilityPercentage = totalItems > 0
                                ? Math.round((double) availableItems / totalItems * 100.0 * 100.0) / 100.0
                                : 0.0;
                        public final String lastUpdated = enrichedSavedItems.getLastUpdated().toString();
                    };

                    log.info("Availability summary for user {}: {}/{} items available ({}%)",
                            userId, summary.availableItems, summary.totalItems, summary.availabilityPercentage);

                    return ResponseEntity.ok(summary);
                })
                .onErrorResume(error -> {
                    log.error("Error fetching availability summary for user {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @Operation(
            summary = "Health check endpoint",
            description = "Simple health check for the Save4Later BFF service"
    )
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> healthCheck() {
        return Mono.just(ResponseEntity.ok("Save4Later BFF Service is healthy"));
    }
}