// Cart-Service/src/test/java/com/Ecommerce/Cart/Service/Services/SavedForLaterServiceTest.java
package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Exception.ResourceNotFoundException;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Repositories.SavedForLaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Saved For Later Service Tests")
class SavedForLaterServiceTest {

    @Mock
    private SavedForLaterRepository savedForLaterRepository;

    @Mock
    private ShoppingCartService cartService;

    @InjectMocks
    private SavedForLaterService savedForLaterService;

    private UUID userId;
    private UUID productId;
    private SavedForLater testSavedItem;
    private ShoppingCart testCart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        testSavedItem = SavedForLater.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(productId)
                .savedAt(LocalDateTime.now())
                .build();

        testCart = ShoppingCart.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();
    }

    @Test
    @DisplayName("Should get saved items for user")
    void getSavedItems_WithValidUserId_ShouldReturnSavedItems() {
        // Arrange
        List<SavedForLater> savedItems = List.of(testSavedItem);
        when(savedForLaterRepository.findByUserId(userId)).thenReturn(savedItems);

        // Act
        List<SavedForLater> result = savedForLaterService.getSavedItems(userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("Should save item for later successfully")
    void saveForLater_WithNewItem_ShouldSaveSuccessfully() {
        // Arrange
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(savedForLaterRepository.save(any(SavedForLater.class))).thenReturn(testSavedItem);

        // Act
        SavedForLater result = savedForLaterService.saveForLater(userId, productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getProductId()).isEqualTo(productId);
        verify(savedForLaterRepository).save(any(SavedForLater.class));
    }

    @Test
    @DisplayName("Should throw exception when saving already saved item")
    void saveForLater_WithAlreadySavedItem_ShouldThrowException() {
        // Arrange
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> savedForLaterService.saveForLater(userId, productId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item is already saved for later");

        verify(savedForLaterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should move item to cart successfully")
    void moveToCart_WithValidItem_ShouldMoveSuccessfully() {
        // Arrange
        BigDecimal price = new BigDecimal("25.99");
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);
        when(cartService.addItemToCart(userId, productId, 1, price)).thenReturn(testCart);

        // Act
        ShoppingCart result = savedForLaterService.moveToCart(userId, productId, price);

        // Assert
        assertThat(result).isNotNull();
        verify(cartService).addItemToCart(userId, productId, 1, price);
        verify(savedForLaterRepository).deleteByUserIdAndProductId(userId, productId);
    }

    @Test
    @DisplayName("Should throw exception when moving non-existent saved item")
    void moveToCart_WithNonExistentItem_ShouldThrowException() {
        // Arrange
        BigDecimal price = new BigDecimal("25.99");
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> savedForLaterService.moveToCart(userId, productId, price))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Saved item not found for product: " + productId);

        verify(cartService, never()).addItemToCart(any(), any(), anyInt(), any());
        verify(savedForLaterRepository, never()).deleteByUserIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("Should remove saved item successfully")
    void removeFromSaved_WithValidItem_ShouldRemoveSuccessfully() {
        // Arrange
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

        // Act
        savedForLaterService.removeFromSaved(userId, productId);

        // Assert
        verify(savedForLaterRepository).deleteByUserIdAndProductId(userId, productId);
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent saved item")
    void removeFromSaved_WithNonExistentItem_ShouldThrowException() {
        // Arrange
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> savedForLaterService.removeFromSaved(userId, productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Saved item not found for product: " + productId);

        verify(savedForLaterRepository, never()).deleteByUserIdAndProductId(any(), any());
    }

    @Test
    @DisplayName("Should clear all saved items")
    void clearAllSavedItems_WithItems_ShouldClearSuccessfully() {
        // Arrange
        when(savedForLaterRepository.countByUserId(userId)).thenReturn(2L);

        // Act
        savedForLaterService.clearAllSavedItems(userId);

        // Assert
        verify(savedForLaterRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("Should get saved items count")
    void getSavedItemCount_ShouldReturnCorrectCount() {
        // Arrange
        long expectedCount = 3L;
        when(savedForLaterRepository.countByUserId(userId)).thenReturn(expectedCount);

        // Act
        long result = savedForLaterService.getSavedItemCount(userId);

        // Assert
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("Should check if product is saved")
    void isProductSaved_WithSavedProduct_ShouldReturnTrue() {
        // Arrange
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

        // Act
        boolean result = savedForLaterService.isProductSaved(userId, productId);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if product is not saved")
    void isProductSaved_WithUnsavedProduct_ShouldReturnFalse() {
        // Arrange
        when(savedForLaterRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);

        // Act
        boolean result = savedForLaterService.isProductSaved(userId, productId);

        // Assert
        assertThat(result).isFalse();
    }
}