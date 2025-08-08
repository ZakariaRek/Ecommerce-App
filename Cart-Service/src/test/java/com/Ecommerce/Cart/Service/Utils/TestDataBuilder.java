package com.Ecommerce.Cart.Service.Utils;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.AddItemRequest;
import com.Ecommerce.Cart.Service.Payload.Request.SaveForLaterRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test data builder for creating test objects
 */
public class TestDataBuilder {

    public static class CartItemBuilder {
        private UUID id = UUID.randomUUID();
        private UUID cartId = UUID.randomUUID();
        private UUID productId = UUID.randomUUID();
        private int quantity = 1;
        private BigDecimal price = new BigDecimal("10.00");
        private LocalDateTime addedAt = LocalDateTime.now();

        public CartItemBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public CartItemBuilder withCartId(UUID cartId) {
            this.cartId = cartId;
            return this;
        }

        public CartItemBuilder withProductId(UUID productId) {
            this.productId = productId;
            return this;
        }

        public CartItemBuilder withQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public CartItemBuilder withPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public CartItemBuilder withAddedAt(LocalDateTime addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public CartItem build() {
            return CartItem.builder()
                    .id(id)
                    .cartId(cartId)
                    .productId(productId)
                    .quantity(quantity)
                    .price(price)
                    .addedAt(addedAt)
                    .build();
        }
    }

    public static class ShoppingCartBuilder {
        private UUID id = UUID.randomUUID();
        private UUID userId = UUID.randomUUID();
        private List<CartItem> items = new ArrayList<>();
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        public ShoppingCartBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public ShoppingCartBuilder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public ShoppingCartBuilder withItems(List<CartItem> items) {
            this.items = items;
            return this;
        }

        public ShoppingCartBuilder withItem(CartItem item) {
            this.items.add(item);
            return this;
        }

        public ShoppingCartBuilder withCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ShoppingCartBuilder withUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ShoppingCartBuilder withExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public ShoppingCart build() {
            return ShoppingCart.builder()
                    .id(id)
                    .userId(userId)
                    .items(items)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .expiresAt(expiresAt)
                    .build();
        }
    }

    public static class SavedForLaterBuilder {
        private UUID id = UUID.randomUUID();
        private UUID userId = UUID.randomUUID();
        private UUID productId = UUID.randomUUID();
        private LocalDateTime savedAt = LocalDateTime.now();

        public SavedForLaterBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public SavedForLaterBuilder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public SavedForLaterBuilder withProductId(UUID productId) {
            this.productId = productId;
            return this;
        }

        public SavedForLaterBuilder withSavedAt(LocalDateTime savedAt) {
            this.savedAt = savedAt;
            return this;
        }

        public SavedForLater build() {
            return SavedForLater.builder()
                    .id(id)
                    .userId(userId)
                    .productId(productId)
                    .savedAt(savedAt)
                    .build();
        }
    }

    public static class AddItemRequestBuilder {
        private UUID productId = UUID.randomUUID();
        private Integer quantity = 1;
        private BigDecimal price = new BigDecimal("10.00");

        public AddItemRequestBuilder withProductId(UUID productId) {
            this.productId = productId;
            return this;
        }

        public AddItemRequestBuilder withQuantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public AddItemRequestBuilder withPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public AddItemRequest build() {
            return AddItemRequest.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .price(price)
                    .build();
        }
    }

    public static class SaveForLaterRequestBuilder {
        private UUID productId = UUID.randomUUID();

        public SaveForLaterRequestBuilder withProductId(UUID productId) {
            this.productId = productId;
            return this;
        }

        public SaveForLaterRequest build() {
            return SaveForLaterRequest.builder()
                    .productId(productId)
                    .build();
        }
    }

    // Static methods for convenience
    public static CartItemBuilder cartItem() {
        return new CartItemBuilder();
    }

    public static ShoppingCartBuilder shoppingCart() {
        return new ShoppingCartBuilder();
    }

    public static SavedForLaterBuilder savedForLater() {
        return new SavedForLaterBuilder();
    }

    public static AddItemRequestBuilder addItemRequest() {
        return new AddItemRequestBuilder();
    }

    public static SaveForLaterRequestBuilder saveForLaterRequest() {
        return new SaveForLaterRequestBuilder();
    }
}