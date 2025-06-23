package com.Ecommerce.Cart.Service.Controllers;

import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.AddItemRequest;
import com.Ecommerce.Cart.Service.Payload.Request.MoveToCartRequest;
import com.Ecommerce.Cart.Service.Payload.Request.SaveForLaterRequest;
import com.Ecommerce.Cart.Service.Payload.Request.UpdateQuantityRequest;
import com.Ecommerce.Cart.Service.Payload.Response.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Shopping Cart", description = "Cart management endpoints")
@RequiredArgsConstructor
public class ShoppingCartController {
    private final ShoppingCartService cartService;
    private final SavedForLaterService savedForLaterService;

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
            @PathVariable UUID userId) {
        ShoppingCart cart = cartService.getOrCreateCart(userId);
        ShoppingCartResponse response = mapToCartResponse(cart);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(response));
    }

    @Operation(
            summary = "Add item to cart",
            description = "Adds a new item to the user's shopping cart or updates quantity if already exists"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Item successfully added to cart",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShoppingCartResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{userId}/items")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> addItemToCart(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Parameter(description = "Item details", required = true)
            @Valid @RequestBody AddItemRequest request) {
        cartService.addItemToCart(userId, request.getProductId(), request.getQuantity(), request.getPrice());
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item added to cart", mapToCartResponse(updatedCart)));
    }

    @Operation(
            summary = "Remove item from cart",
            description = "Removes an item from the shopping cart based on product ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Item successfully removed from cart",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShoppingCartResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Item not found in cart"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> removeItemFromCart(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Parameter(description = "Product ID to remove", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID productId) {
        cartService.removeItemFromCart(userId, productId);
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item removed from cart", mapToCartResponse(updatedCart)));
    }

    @Operation(
            summary = "Update item quantity",
            description = "Updates the quantity of a specific item in the shopping cart"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Item quantity successfully updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShoppingCartResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid quantity (must be at least 1)"),
            @ApiResponse(responseCode = "404", description = "Item not found in cart"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<ShoppingCartResponse>> updateItemQuantity(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Parameter(description = "Product ID to update", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID productId,
            @Parameter(description = "New quantity details", required = true)
            @Valid @RequestBody UpdateQuantityRequest request) {
        cartService.updateItemQuantity(userId, productId, request.getQuantity());
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item quantity updated", mapToCartResponse(updatedCart)));
    }

    @Operation(
            summary = "Get cart total",
            description = "Calculates and returns the total price of all items in the shopping cart"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Total calculated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CartTotalResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/{userId}/total")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<CartTotalResponse>> getCartTotal(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        CartTotalResponse response = CartTotalResponse.builder()
                .total(cartService.calculateCartTotal(userId))
                .build();
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success(response));
    }

    @Operation(
            summary = "Checkout cart",
            description = "Process the checkout for the user's shopping cart"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Checkout completed successfully",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "400", description = "Invalid cart state"),
            @ApiResponse(responseCode = "404", description = "Cart not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{userId}/checkout")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<Void>> checkout(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        cartService.checkout(userId);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Checkout completed successfully", null));
    }

    // Saved for later endpoints

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
            @PathVariable UUID userId) {
        List<SavedForLater> savedItems = savedForLaterService.getSavedItems(userId);
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
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/{userId}/saved")
    public ResponseEntity<com.Ecommerce.Cart.Service.Payload.Response.ApiResponse<SavedItemResponse>> saveForLater(
            @Parameter(description = "User ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Parameter(description = "Product details to save", required = true)
            @Valid @RequestBody SaveForLaterRequest request) {
        SavedForLater savedItem = savedForLaterService.saveForLater(userId, request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item saved for later", mapToSavedItemResponse(savedItem)));
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
            @PathVariable UUID userId,
            @Parameter(description = "Product ID to move to cart", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID productId,
            @Parameter(description = "Price details", required = true)
            @Valid @RequestBody MoveToCartRequest request) {
        savedForLaterService.moveToCart(userId, productId, request.getPrice());
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
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
            @PathVariable UUID userId,
            @Parameter(description = "Product ID to remove", required = true, example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID productId) {
        savedForLaterService.removeFromSaved(userId, productId);
        return ResponseEntity.ok(com.Ecommerce.Cart.Service.Payload.Response.ApiResponse.success("Item removed from saved items", null));
    }

    // Helper methods for mapping entities to DTOs
    private ShoppingCartResponse mapToCartResponse(ShoppingCart cart) {
        return ShoppingCartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(cart.getItems().stream()
                        .map(item -> CartItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .subtotal(item.getSubtotal())
                                .addedAt(item.getAddedAt())
                                .build())
                        .collect(Collectors.toList()))
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
}