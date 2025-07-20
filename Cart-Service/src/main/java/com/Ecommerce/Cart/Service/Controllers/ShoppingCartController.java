package com.Ecommerce.Cart.Service.Controllers;

import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.*;
import com.Ecommerce.Cart.Service.Payload.Response.*;
import com.Ecommerce.Cart.Service.Services.CartSyncService;
import com.Ecommerce.Cart.Service.Services.SavedForLaterService;
import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Shopping Cart", description = "Cart management endpoints")
@RequiredArgsConstructor
@Slf4j
public class ShoppingCartController {
    private final ShoppingCartService cartService;
    private final SavedForLaterService savedForLaterService;
    private final CartSyncService cartSyncService;

    @Operation(
            summary = "Get cart by user ID",
            description = "Retrieves a shopping cart by user ID or creates a new one if it doesn't exist"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved cart",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShoppingCartResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> getCart(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        try {
                UUID parsedUserId = parseUUID(userId);
            ShoppingCart cart = cartService.getOrCreateCart(parsedUserId);
            ShoppingCartResponse response = mapToCartResponse(cart);
            return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
        }
    }

    private UUID parseUUID(String uuidString) {
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

    @PostMapping("/{userId}/items")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> addItemToCart(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "Item details", required = true)
            @Valid @RequestBody AddItemRequest request) {
        try {
            UUID parsedUserId = parseUUID(userId);
            cartService.addItemToCart(parsedUserId, request.getProductId(), request.getQuantity(), request.getPrice());
            ShoppingCart updatedCart = cartService.getOrCreateCart(parsedUserId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item added to cart", mapToCartResponse(updatedCart)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
        }
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> removeItemFromCart(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "Product ID to remove", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable String productId) {
        try {
            UUID parsedUserId = parseUUID(userId);
            UUID parsedProductId = parseUUID(productId);
            cartService.removeItemFromCart(parsedUserId, parsedProductId);
            ShoppingCart updatedCart = cartService.getOrCreateCart(parsedUserId);
            return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item removed from cart", mapToCartResponse(updatedCart)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format"));
        }
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> updateItemQuantity(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "Product ID to update", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable String productId,
            @Parameter(description = "New quantity details", required = true)
            @Valid @RequestBody UpdateQuantityRequest request) {
        try {
            UUID parsedUserId = parseUUID(userId);
            UUID parsedProductId = parseUUID(productId);
            cartService.updateItemQuantity(parsedUserId, parsedProductId, request.getQuantity());
            ShoppingCart updatedCart = cartService.getOrCreateCart(parsedUserId);
            return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item quantity updated", mapToCartResponse(updatedCart)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format"));
        }
    }

    @GetMapping("/{userId}/total")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<CartTotalResponse>> getCartTotal(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        try {
            UUID parsedUserId = parseUUID(userId);
            CartTotalResponse response = CartTotalResponse.builder()
                    .total(cartService.calculateCartTotal(parsedUserId))
                    .build();
            return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
        }
    }
    @PostMapping("/{userId}/checkout")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<Void>> checkout(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        cartService.checkout(userId);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Checkout completed successfully", null));
    }





    //                                Saved for later endpoints

    @Operation(
            summary = "Get saved items",
            description = "Retrieves all items saved for later by the user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Saved items retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SavedItemResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/{userId}/saved")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<List<SavedItemResponse>>> getSavedItems(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        UUID parsedUserId = parseUUID(userId);
        List<SavedForLater> savedItems = savedForLaterService.getSavedItems(parsedUserId);
        List<SavedItemResponse> response = savedItems.stream()
                .map(this::mapToSavedItemResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(response));
    }
    @Operation(
            summary = "Save item for later",
            description = "Saves an item for later purchase"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Item saved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SavedItemResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Item already saved"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{userId}/saved")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<SavedItemResponse>> saveForLater(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "Product details to save", required = true)
            @Valid @RequestBody SaveForLaterRequest request) {

        try {
            // ✅ FIXED: Parse userId first and handle errors
            UUID parsedUserId = parseUUID(userId);

            // ✅ FIXED: Validate request data properly
            if (request.getProductId() == null) {
                return ResponseEntity.badRequest()
                        .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Product ID is required"));
            }

            // ✅ FIXED: Don't parse productId again since SaveForLaterRequest.getProductId() already returns UUID
            UUID productId = request.getProductId();

            // ✅ FIXED: Call service method with proper error handling
            SavedForLater savedItem = savedForLaterService.saveForLater(parsedUserId, productId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(
                            "Item saved for later",
                            mapToSavedItemResponse(savedItem)));

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("UUID format")) {
                return ResponseEntity.badRequest()
                        .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
            } else if (e.getMessage().contains("already saved")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Item is already saved for later"));
            } else {
                return ResponseEntity.badRequest()
                        .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error(e.getMessage()));
            }
        } catch (Exception e) {
            log.error("Error saving item for later: userId={}, productId={}", userId, request.getProductId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Failed to save item for later"));
        }
    }
    @Operation(
            summary = "Move saved item to cart",
            description = "Moves an item from saved-for-later to the active shopping cart"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Item moved to cart successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShoppingCartResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Saved item not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{userId}/saved/{productId}/move-to-cart")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> moveToCart(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "Product ID to move to cart", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable String productId,
            @Parameter(description = "Price details", required = true)
            @Valid @RequestBody MoveToCartRequest request) {
        savedForLaterService.moveToCart(parseUUID(userId), parseUUID(productId), request.getPrice());
        ShoppingCart updatedCart = cartService.getOrCreateCart(parseUUID(userId));
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item moved to cart", mapToCartResponse(updatedCart)));
    }

    @Operation(
            summary = "Remove saved item",
            description = "Removes an item from the saved-for-later list"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Saved item removed successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "404", description = "Saved item not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @DeleteMapping("/{userId}/saved/{productId}")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<Void>> removeFromSaved(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "Product ID to remove", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable String productId) {
        savedForLaterService.removeFromSaved(parseUUID(userId), parseUUID(productId));
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item removed from saved items", null));
    }




    @Operation(
            summary = "Clear all saved items",
            description = "Removes all saved items for the user"
    )
    @DeleteMapping("/{userId}/saved")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<Void>> clearAllSavedItems(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        try {
            UUID parsedUserId = parseUUID(userId);
            savedForLaterService.clearAllSavedItems(parsedUserId);
            return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("All saved items cleared", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Failed to clear saved items"));
        }
    }

    @Operation(
            summary = "Get saved items count",
            description = "Returns the number of items saved for later by the user"
    )
    @GetMapping("/{userId}/saved/count")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<Long>> getSavedItemsCount(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        try {
            UUID parsedUserId = parseUUID(userId);
            long count = savedForLaterService.getSavedItemCount(parsedUserId);
            return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
        }
    }

    @Operation(
            summary = "Check if product is saved",
            description = "Checks if a specific product is saved for later by the user"
    )
    @GetMapping("/{userId}/saved/{productId}/exists")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<Boolean>> isProductSaved(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "Product ID to check", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable String productId) {
        try {
            UUID parsedUserId = parseUUID(userId);
            UUID parsedProductId = parseUUID(productId);
            boolean isSaved = savedForLaterService.isProductSaved(parsedUserId, parsedProductId);
            return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(isSaved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format"));
        }
    }

    @Operation(
            summary = "Bulk save items for later",
            description = "Saves multiple items for later in a single request"
    )
    @PostMapping("/{userId}/saved/bulk")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<List<SavedItemResponse>>> bulkSaveForLater(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId,
            @Parameter(description = "List of product IDs to save", required = true)
            @Valid @RequestBody BulkSaveForLaterRequest request) {
        try {
            UUID parsedUserId = parseUUID(userId);
            List<SavedItemResponse> savedItems = new ArrayList<>();
            int successCount = 0;
            int skipCount = 0;

            for (UUID productId : request.getProductIds()) {
                try {
                    SavedForLater savedItem = savedForLaterService.saveForLater(parsedUserId, productId);
                    savedItems.add(mapToSavedItemResponse(savedItem));
                    successCount++;
                } catch (IllegalArgumentException e) {
                    if (e.getMessage().contains("already saved")) {
                        skipCount++;
                    } else {
                        throw e; // Re-throw other validation errors
                    }
                }
            }

            String message = String.format("Saved %d items for later", successCount);
            if (skipCount > 0) {
                message += String.format(" (%d items were already saved)", skipCount);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(message, savedItems));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Failed to save items for later"));
        }
    }
    // Helper methods for mapping entities to DTOs
    private ShoppingCartResponse mapToCartResponse(ShoppingCart cart) {
        return ShoppingCartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(cart.getItems() != null ? cart.getItems().stream()
                        .map(item -> CartItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .subtotal(item.getSubtotal())
                                .addedAt(item.getAddedAt())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .total(cart.calculateTotal())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expiresAt(cart.getExpiresAt())
                .build();
    }
    private SavedItemResponse mapToSavedItemResponse(SavedForLater savedItem) {
        return SavedItemResponse.builder()
                .id(savedItem.getId())
                .productId(savedItem.getProductId())
                .savedAt(savedItem.getSavedAt())
                .build();
    }

@PostMapping("/{userId}/sync")
@Operation(summary = "Sync localStorage cart with server",
        description = "Merges guest cart from localStorage with server cart on login")
public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> syncCart(
        @Parameter(description = "User ID", required = true)
        @PathVariable String userId,
        @Valid @RequestBody CartSyncRequest syncRequest) {
    try {
        UUID parsedUserId = parseUUID(userId);

        // Get or create server cart
        ShoppingCart serverCart = cartService.getOrCreateCart(parsedUserId);

        // Merge with localStorage cart
        ShoppingCart mergedCart = cartSyncService.mergeWithLocalStorage(
                serverCart, syncRequest ,parsedUserId);

        ShoppingCartResponse response = mapToCartResponse(mergedCart);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(
                "Cart synchronized successfully", response));

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Sync failed: " + e.getMessage()));
    }
}

@PostMapping("/guest/validate")
@Operation(summary = "Validate guest cart",
        description = "Validates localStorage cart items for price/availability changes")
public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<CartValidationResponse>> validateGuestCart(
        @Valid @RequestBody GuestCartRequest guestCart) {
    try {
        CartValidationResponse validation = cartSyncService.validateGuestCart(guestCart);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(validation));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Validation failed: " + e.getMessage()));
    }
}

@PutMapping("/{userId}/bulk-update")
@Operation(summary = "Bulk update cart items")
public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> bulkUpdateCart(
        @PathVariable String userId,
        @Valid @RequestBody BulkUpdateRequest request) {
    try {
        UUID parsedUserId = parseUUID(userId);
        ShoppingCart updatedCart = cartService.bulkUpdateCart(parsedUserId, request);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(
                "Cart updated successfully", mapToCartResponse(updatedCart)));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Invalid UUID format: " + userId));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Bulk update failed: " + e.getMessage()));
    }
}

@GetMapping("/guest/session/{sessionId}")
@Operation(summary = "Get guest cart by session ID")
public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> getGuestCart(
        @PathVariable String sessionId) {
    try {
        // For demo purposes - in real implementation, you might store guest carts temporarily
        // or just return validation info
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(
                "Guest cart session: " + sessionId, null));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.error("Failed to get guest cart: " + e.getMessage()));
    }
}






}