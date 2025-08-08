package com.Ecommerce.Gateway_Service.TestUtils;

import com.Ecommerce.Gateway_Service.DTOs.Cart.EnrichedCartItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.EnrichedShoppingCartResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TestDataBuilder {

    public static EnrichedShoppingCartResponse.EnrichedCartResponseDTO buildTestCart(UUID userId) {
        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .items(buildTestCartItems())
                .total(BigDecimal.valueOf(99.98))
                .itemCount(2)
                .totalQuantity(3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static List<EnrichedCartItemDTO> buildTestCartItems() {
        return List.of(
                EnrichedCartItemDTO.builder()
                        .id(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .quantity(2)
                        .price(BigDecimal.valueOf(29.99))
                        .subtotal(BigDecimal.valueOf(59.98))
                        .productName("Test Product 1")
                        .inStock(true)
                        .availableQuantity(10)
                        .build(),
                EnrichedCartItemDTO.builder()
                        .id(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .quantity(1)
                        .price(BigDecimal.valueOf(39.99))
                        .subtotal(BigDecimal.valueOf(39.99))
                        .productName("Test Product 2")
                        .inStock(false)
                        .availableQuantity(0)
                        .build()
        );
    }
}