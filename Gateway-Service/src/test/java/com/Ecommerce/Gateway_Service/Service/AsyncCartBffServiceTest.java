package com.Ecommerce.Gateway_Service.Service;

import com.Ecommerce.Gateway_Service.DTOs.Cart.EnrichedCartItemDTO;
import com.Ecommerce.Gateway_Service.DTOs.EnrichedShoppingCartResponse;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncCartBffServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private AsyncResponseManager asyncResponseManager;

    @Mock
    private AsyncProductService asyncProductService;

    @Mock
    private ObjectMapper objectMapper;

    private AsyncCartBffService asyncCartBffService;

    @BeforeEach
    void setUp() {
        asyncCartBffService = new AsyncCartBffService(
                kafkaTemplate, asyncResponseManager, asyncProductService, objectMapper
        );
    }

    @Test
    void shouldReturnEnrichedCartWithProducts() {
        // Given
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        UUID userUuid = UUID.fromString(userId);

        EnrichedShoppingCartResponse.EnrichedCartResponseDTO basicCart = createBasicCartResponse(userUuid);
        List<EnrichedCartItemDTO> productItems = createProductItems();

        when(asyncResponseManager.waitForResponse(anyString(), any(Duration.class), eq(EnrichedShoppingCartResponse.EnrichedCartResponseDTO.class)))
                .thenReturn(Mono.just(basicCart));
        when(asyncProductService.getProductsBatch(anyList()))
                .thenReturn(Mono.just(productItems));

        // When
        Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> result =
                asyncCartBffService.getEnrichedCartWithProducts(userId);

        // Then
        StepVerifier.create(result)
                .assertNext(enrichedCart -> {
                    assertThat(enrichedCart.getUserId()).isEqualTo(userUuid);
                    assertThat(enrichedCart.getItemCount()).isEqualTo(1);
                    assertThat(enrichedCart.getItems()).hasSize(1);

                    EnrichedCartItemDTO item = enrichedCart.getItems().get(0);
                    assertThat(item.getProductName()).isEqualTo("Test Product");
                    assertThat(item.getInStock()).isTrue();
                })
                .verifyComplete();

        verify(kafkaTemplate).send(eq("cart.request"), anyString(), any());
    }

    @Test
    void shouldHandleEmptyCart() {
        // Given
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        UUID userUuid = UUID.fromString(userId);

        EnrichedShoppingCartResponse.EnrichedCartResponseDTO emptyCart = createEmptyCartResponse(userUuid);

        when(asyncResponseManager.waitForResponse(anyString(), any(Duration.class), eq(EnrichedShoppingCartResponse.EnrichedCartResponseDTO.class)))
                .thenReturn(Mono.just(emptyCart));

        // When
        Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> result =
                asyncCartBffService.getEnrichedCartWithProducts(userId);

        // Then
        StepVerifier.create(result)
                .assertNext(cart -> {
                    assertThat(cart.getUserId()).isEqualTo(userUuid);
                    assertThat(cart.getItemCount()).isEqualTo(0);
                    assertThat(cart.getItems()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleInvalidUserId() {
        // Given
        String invalidUserId = "";

        // When
        Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> result =
                asyncCartBffService.getEnrichedCartWithProducts(invalidUserId);

        // Then
        StepVerifier.create(result)
                .assertNext(cart -> {
                    assertThat(cart.getUserId()).isNull();
                    assertThat(cart.getItemCount()).isEqualTo(0);
                    assertThat(cart.getItems()).isEmpty();
                })
                .verifyComplete();
    }

    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO createBasicCartResponse(UUID userId) {
        EnrichedCartItemDTO item = EnrichedCartItemDTO.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(BigDecimal.valueOf(29.99))
                .subtotal(BigDecimal.valueOf(59.98))
                .addedAt(LocalDateTime.now())
                .build();

        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .items(List.of(item))
                .total(BigDecimal.valueOf(59.98))
                .itemCount(1)
                .totalQuantity(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO createEmptyCartResponse(UUID userId) {
        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .items(List.of())
                .total(BigDecimal.ZERO)
                .itemCount(0)
                .totalQuantity(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private List<EnrichedCartItemDTO> createProductItems() {
        EnrichedCartItemDTO productItem = EnrichedCartItemDTO.builder()
                .productId(UUID.randomUUID())
                .productName("Test Product")
                .productImage("/images/test.jpg")
                .inStock(true)
                .availableQuantity(10)
                .productStatus("AVAILABLE")
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(10))
                .build();

        return List.of(productItem);
    }
}