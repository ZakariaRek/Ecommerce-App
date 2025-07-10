package com.Ecommerce.Gateway_Service.Service;

import com.Ecommerce.Gateway_Service.DTOs.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartBffService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${services.cart-service.url:http://cart-service}")
    private String cartServiceUrl;

    @Value("${services.product-service.url:http://product-service}")
    private String productServiceUrl;

    // ✅ COMPLETELY REACTIVE - NO .block() ANYWHERE
    public Mono<EnrichedShoppingCartResponse.EnrichedCartResponseDTO> getEnrichedCart(String userId) {
        log.info("Starting enriched cart fetch for user: {}", userId);

        return getCartFromService(userId)
                .flatMap(cartResponse -> {
                    if (cartResponse.getData() == null ||
                            cartResponse.getData().getItems() == null ||
                            cartResponse.getData().getItems().isEmpty()) {

                        log.info("Empty cart for user: {}", userId);
                        return Mono.just(createEmptyEnrichedCart(userId));
                    }

                    List<UUID> productIds = cartResponse.getData().getItems().stream()
                            .map(CartItemDTO::getProductId)
                            .collect(Collectors.toList());

                    log.info("Found {} unique products in cart for user: {}", productIds.size(), userId);

                    return getProductInfoBatch(productIds)
                            .map(productInfos -> mergeCartWithProductInfo(cartResponse.getData(), productInfos));
                })
                .timeout(Duration.ofSeconds(10))
                .doOnError(error -> log.error("Error in enriched cart fetch for user {}: {}", userId, error.getMessage()))
                .onErrorReturn(createEmptyEnrichedCart(userId));
    }

    private Mono<CartServiceResponseDTO> getCartFromService(String userId) {
        WebClient webClient = webClientBuilder.baseUrl(cartServiceUrl).build();

        return webClient.get()
                .uri("/api/carts/{userId}", userId)
                .retrieve()
                .bodyToMono(CartServiceResponseDTO.class)
                .doOnNext(response -> log.debug("Cart service response: {}", response))
                .onErrorReturn(createEmptyCartServiceResponse(userId));
    }

    private Mono<List<ProductBatchInfoDTO>> getProductInfoBatch(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Mono.just(List.of());
        }

        WebClient webClient = webClientBuilder.baseUrl(productServiceUrl).build();

        ProductBatchRequestDTO request = new ProductBatchRequestDTO();
        request.setProductIds(productIds);

        return webClient.post()
                .uri("/api/products/batch/product-info")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    try {
                        return objectMapper.readValue(responseBody, new TypeReference<List<ProductBatchInfoDTO>>() {});
                    } catch (Exception e) {
                        log.error("Error deserializing product service response: {}", e.getMessage());
                        return List.<ProductBatchInfoDTO>of();
                    }
                })
                .doOnNext(productInfos -> log.debug("Product service returned {} products", productInfos.size()))
                .onErrorReturn(List.of());
    }

    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO mergeCartWithProductInfo(ShoppingCartDTO cart, List<ProductBatchInfoDTO> productInfos) {
        log.info("Merging cart data with product information");

        Map<UUID, ProductBatchInfoDTO> productMap = productInfos.stream()
                .collect(Collectors.toMap(ProductBatchInfoDTO::getId, p -> p));

        List<EnrichedCartItemDTO> enrichedItems = cart.getItems().stream()
                .map(cartItem -> enrichCartItem(cartItem, productMap.get(cartItem.getProductId())))
                .collect(Collectors.toList());

        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(enrichedItems)
                .total(cart.getTotal())
                .itemCount(enrichedItems.size())
                .totalQuantity(enrichedItems.stream().mapToInt(EnrichedCartItemDTO::getQuantity).sum())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expiresAt(cart.getExpiresAt())
                .build();
    }

    private EnrichedCartItemDTO enrichCartItem(CartItemDTO cartItem, ProductBatchInfoDTO productInfo) {
        EnrichedCartItemDTO.EnrichedCartItemDTOBuilder builder = EnrichedCartItemDTO.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProductId())
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPrice())
                .subtotal(cartItem.getSubtotal())
                .addedAt(cartItem.getAddedAt());

        if (productInfo != null) {
            builder
                    .productName(productInfo.getName())
                    .productImage(productInfo.getImagePath())
                    .inStock(productInfo.getInStock())
                    .availableQuantity(productInfo.getAvailableQuantity())
                    .productStatus(productInfo.getStatus().toString());
        } else {
            builder
                    .productName("Product Not Found")
                    .productImage("/api/products/images/not-found.png")
                    .inStock(false)
                    .availableQuantity(0)
                    .productStatus("NOT_FOUND");
        }

        return builder.build();
    }


    private EnrichedShoppingCartResponse.EnrichedCartResponseDTO createEmptyEnrichedCart(String userId) {
        UUID parsedUserId;
        try {
            // Try to parse as UUID first
            parsedUserId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // If it's not a valid UUID (like MongoDB ObjectId), generate a random UUID
            parsedUserId = UUID.randomUUID();
            log.warn("Invalid UUID format for userId: {}, using random UUID", userId);
        }

        return EnrichedShoppingCartResponse.EnrichedCartResponseDTO.builder()
                .userId(parsedUserId)
                .items(List.of())
                .total(java.math.BigDecimal.ZERO)
                .itemCount(0)
                .totalQuantity(0)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }


    private CartServiceResponseDTO createEmptyCartServiceResponse(String userId) {
        CartServiceResponseDTO response = new CartServiceResponseDTO();
        response.setSuccess(false);
        response.setMessage("Cart service unavailable");

        ShoppingCartDTO emptyCart = new ShoppingCartDTO();

        // Handle UUID parsing safely
        try {
            emptyCart.setUserId(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            // If it's not a valid UUID (like MongoDB ObjectId), generate a random UUID
            emptyCart.setUserId(UUID.randomUUID());
            log.warn("Invalid UUID format for userId: {}, using random UUID", userId);
        }

        emptyCart.setItems(List.of());
        emptyCart.setTotal(java.math.BigDecimal.ZERO);
        emptyCart.setCreatedAt(java.time.LocalDateTime.now());
        emptyCart.setUpdatedAt(java.time.LocalDateTime.now());

        response.setData(emptyCart);
        return response;
    }
}