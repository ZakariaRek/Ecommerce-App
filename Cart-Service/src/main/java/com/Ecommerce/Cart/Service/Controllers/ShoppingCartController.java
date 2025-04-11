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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class ShoppingCartController {
    private final ShoppingCartService cartService;
    private final SavedForLaterService savedForLaterService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ShoppingCartResponse>> getCart(@PathVariable UUID userId) {
        ShoppingCart cart = cartService.getOrCreateCart(userId);
        ShoppingCartResponse response = mapToCartResponse(cart);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<ApiResponse<ShoppingCartResponse>> addItemToCart(
            @PathVariable UUID userId,
            @Valid @RequestBody AddItemRequest request) {
        cartService.addItemToCart(userId, request.getProductId(), request.getQuantity(), request.getPrice());
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart", mapToCartResponse(updatedCart)));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<ShoppingCartResponse>> removeItemFromCart(
            @PathVariable UUID userId,
            @PathVariable UUID productId) {
        cartService.removeItemFromCart(userId, productId);
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", mapToCartResponse(updatedCart)));
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<ShoppingCartResponse>> updateItemQuantity(
            @PathVariable UUID userId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        cartService.updateItemQuantity(userId, productId, request.getQuantity());
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Item quantity updated", mapToCartResponse(updatedCart)));
    }

    @GetMapping("/{userId}/total")
    public ResponseEntity<ApiResponse<CartTotalResponse>> getCartTotal(@PathVariable UUID userId) {
        CartTotalResponse response = CartTotalResponse.builder()
                .total(cartService.calculateCartTotal(userId))
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@PathVariable UUID userId) {
        cartService.checkout(userId);
        return ResponseEntity.ok(ApiResponse.success("Checkout completed successfully", null));
    }

    // Saved for later endpoints

    @GetMapping("/{userId}/saved")
    public ResponseEntity<ApiResponse<List<SavedItemResponse>>> getSavedItems(@PathVariable UUID userId) {
        List<SavedForLater> savedItems = savedForLaterService.getSavedItems(userId);
        List<SavedItemResponse> response = savedItems.stream()
                .map(this::mapToSavedItemResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/saved")
    public ResponseEntity<ApiResponse<SavedItemResponse>> saveForLater(
            @PathVariable UUID userId,
            @Valid @RequestBody SaveForLaterRequest request) {
        SavedForLater savedItem = savedForLaterService.saveForLater(userId, request.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item saved for later", mapToSavedItemResponse(savedItem)));
    }

    @PostMapping("/{userId}/saved/{productId}/move-to-cart")
    public ResponseEntity<ApiResponse<ShoppingCartResponse>> moveToCart(
            @PathVariable UUID userId,
            @PathVariable UUID productId,
            @Valid @RequestBody MoveToCartRequest request) {
        savedForLaterService.moveToCart(userId, productId, request.getPrice());
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Item moved to cart", mapToCartResponse(updatedCart)));
    }

    @DeleteMapping("/{userId}/saved/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromSaved(
            @PathVariable UUID userId,
            @PathVariable UUID productId) {
        savedForLaterService.removeFromSaved(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from saved items", null));
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
