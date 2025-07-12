package com.Ecommerce.Cart.Service.Payload.Response;

import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCartResponse {
    private UUID id;
    private UUID userId;
    private List<CartItemResponse> items;
    private BigDecimal total;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    public static ShoppingCartResponse fromShoppingCart(ShoppingCart cart) {
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
}