package com.Ecommerce.Cart.Service.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shopping_carts")
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignore unknown properties during deserialization
public class ShoppingCart {
    @Id
    private UUID id;
    private UUID userId;
    private List<CartItem> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    // Business methods
    public void addItem(CartItem item) {
        // Check if items is still null (defensive programming)
        if (items == null) {
            items = new ArrayList<>();
        }

        for (CartItem existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId())) {
                existingItem.updateQuantity(existingItem.getQuantity() + item.getQuantity());
                this.updatedAt = LocalDateTime.now();
                return;
            }
        }

        item.setCartId(this.id);
        item.setAddedAt(LocalDateTime.now());
        items.add(item);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeItem(UUID productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
        this.updatedAt = LocalDateTime.now();
    }

    public void updateQuantity(UUID productId, int newQuantity) {
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                item.updateQuantity(newQuantity);
                this.updatedAt = LocalDateTime.now();
                return;
            }
        }
    }

    public void applyCoupon(String couponCode) {
        // Implementation depends on coupon system
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal calculateTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void checkout() {
        // Checkout logic
        this.updatedAt = LocalDateTime.now();
    }
}