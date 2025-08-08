// Cart-Service/src/test/java/com/Ecommerce/Cart/Service/Services/ShoppingCartServiceTest.java
package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Exception.ResourceNotFoundException;
import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Repositories.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Shopping Cart Service Tests")
class ShoppingCartServiceTest {

    @Mock
    private ShoppingCartRepository cartRepository;

    @InjectMocks
    private ShoppingCartService cartService;

    private UUID userId;
    private UUID productId;
    private ShoppingCart testCart;
    private CartItem testItem;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        testItem = CartItem.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .quantity(2)
                .price(new BigDecimal("29.99"))
                .addedAt(LocalDateTime.now())
                .build();

        testCart = ShoppingCart.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .items(new ArrayList<>(List.of(testItem)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    @DisplayName("Should return existing cart when cart exists")
    void getOrCreateCart_WhenCartExists_ShouldReturnExistingCart() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        // Act
        ShoppingCart result = cartService.getOrCreateCart(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testCart.getId());
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getItems()).hasSize(1);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create new cart when cart doesn't exist")
    void getOrCreateCart_WhenCartDoesNotExist_ShouldCreateNewCart() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Act
        ShoppingCart result = cartService.getOrCreateCart(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(cartRepository).save(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("Should add new item to cart successfully")
    void addItemToCart_WithNewItem_ShouldAddItemSuccessfully() {
        // Arrange
        UUID newProductId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("15.99");
        int quantity = 1;

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Act
        ShoppingCart result = cartService.addItemToCart(userId, newProductId, quantity, price);

        // Assert
        assertThat(result).isNotNull();
        verify(cartRepository).save(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("Should remove item from cart successfully")
    void removeItemFromCart_WithExistingItem_ShouldRemoveItemSuccessfully() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Act
        ShoppingCart result = cartService.removeItemFromCart(userId, productId);

        // Assert
        assertThat(result).isNotNull();
        verify(cartRepository).save(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("Should update item quantity successfully")
    void updateItemQuantity_WithValidQuantity_ShouldUpdateSuccessfully() {
        // Arrange
        int newQuantity = 5;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Act
        ShoppingCart result = cartService.updateItemQuantity(userId, productId, newQuantity);

        // Assert
        assertThat(result).isNotNull();
        verify(cartRepository).save(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("Should calculate cart total correctly")
    void calculateCartTotal_WithItems_ShouldReturnCorrectTotal() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        // Act
        BigDecimal total = cartService.calculateCartTotal(userId);

        // Assert
        BigDecimal expectedTotal = testItem.getPrice().multiply(BigDecimal.valueOf(testItem.getQuantity()));
        assertThat(total).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("Should return zero total when cart is empty")
    void calculateCartTotal_WithEmptyCart_ShouldReturnZero() {
        // Arrange
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

        // Act
        BigDecimal total = cartService.calculateCartTotal(userId);

        // Assert
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero total when cart doesn't exist")
    void calculateCartTotal_WhenCartDoesNotExist_ShouldReturnZero() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act
        BigDecimal total = cartService.calculateCartTotal(userId);

        // Assert
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should checkout cart successfully")
    void checkout_WithValidCart_ShouldCheckoutSuccessfully() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(ShoppingCart.class))).thenReturn(testCart);

        // Act & Assert
        assertThatNoException().isThrownBy(() -> cartService.checkout(userId));
        verify(cartRepository).save(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent cart")
    void getCart_WhenCartDoesNotExist_ShouldThrowException() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.getCart(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart not found for user: " + userId);
    }

    @Test
    @DisplayName("Should cleanup expired carts")
    void cleanupExpiredCarts_ShouldRemoveExpiredCarts() {
        // Arrange
        List<ShoppingCart> expiredCarts = List.of(testCart);
        when(cartRepository.findByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(expiredCarts);

        // Act
        cartService.cleanupExpiredCarts();

        // Assert
        verify(cartRepository).deleteAll(expiredCarts);
    }
}