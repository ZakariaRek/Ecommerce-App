// Cart-Service/src/test/java/com/Ecommerce/Cart/Service/Controllers/ShoppingCartControllerTest.java
package com.Ecommerce.Cart.Service.Controllers;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.AddItemRequest;
import com.Ecommerce.Cart.Service.Payload.Request.SaveForLaterRequest;
import com.Ecommerce.Cart.Service.Payload.Request.UpdateQuantityRequest;
import com.Ecommerce.Cart.Service.Services.CartSyncService;
import com.Ecommerce.Cart.Service.Services.SavedForLaterService;
import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShoppingCartController.class)
@DisplayName("Shopping Cart Controller Tests")
class ShoppingCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private ShoppingCartService cartService;

    @Mock
    private SavedForLaterService savedForLaterService;

    @Mock
    private CartSyncService cartSyncService;

    private UUID userId;
    private UUID productId;
    private ShoppingCart testCart;
    private CartItem testItem;
    private SavedForLater testSavedItem;

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

        testSavedItem = SavedForLater.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(productId)
                .savedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /userId - Should get cart by user ID")
    void getCart_WithValidUserId_ShouldReturnCart() throws Exception {
        // Arrange
        when(cartService.getOrCreateCart(any(UUID.class))).thenReturn(testCart);

        // Act & Assert
        mockMvc.perform(get("/{userId}", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].price").value(29.99));
    }

    @Test
    @DisplayName("POST /userId/items - Should add item to cart")
    void addItemToCart_WithValidRequest_ShouldAddItem() throws Exception {
        // Arrange
        AddItemRequest request = AddItemRequest.builder()
                .productId(productId)
                .quantity(1)
                .price(new BigDecimal("15.99"))
                .build();

        when(cartService.getOrCreateCart(any(UUID.class))).thenReturn(testCart);

        // Act & Assert
        mockMvc.perform(post("/{userId}/items", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added to cart"));

        verify(cartService).addItemToCart(any(UUID.class), eq(productId), eq(1), eq(new BigDecimal("15.99")));
    }

    @Test
    @DisplayName("POST /userId/items - Should return bad request for invalid data")
    void addItemToCart_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AddItemRequest request = AddItemRequest.builder()
                .productId(null) // Invalid: null productId
                .quantity(-1)    // Invalid: negative quantity
                .price(new BigDecimal("-10.00")) // Invalid: negative price
                .build();

        // Act & Assert
        mockMvc.perform(post("/{userId}/items", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /userId/items/productId - Should remove item from cart")
    void removeItemFromCart_WithValidIds_ShouldRemoveItem() throws Exception {
        // Arrange
        when(cartService.getOrCreateCart(any(UUID.class))).thenReturn(testCart);

        // Act & Assert
        mockMvc.perform(delete("/{userId}/items/{productId}", userId.toString(), productId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item removed from cart"));

        verify(cartService).removeItemFromCart(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("PUT /userId/items/productId - Should update item quantity")
    void updateItemQuantity_WithValidRequest_ShouldUpdateQuantity() throws Exception {
        // Arrange
        UpdateQuantityRequest request = UpdateQuantityRequest.builder()
                .quantity(5)
                .build();

        when(cartService.getOrCreateCart(any(UUID.class))).thenReturn(testCart);

        // Act & Assert
        mockMvc.perform(put("/{userId}/items/{productId}", userId.toString(), productId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item quantity updated"));

        verify(cartService).updateItemQuantity(any(UUID.class), any(UUID.class), eq(5));
    }

    @Test
    @DisplayName("GET /userId/total - Should get cart total")
    void getCartTotal_WithValidUserId_ShouldReturnTotal() throws Exception {
        // Arrange
        BigDecimal expectedTotal = new BigDecimal("59.98");
        when(cartService.calculateCartTotal(any(UUID.class))).thenReturn(expectedTotal);

        // Act & Assert
        mockMvc.perform(get("/{userId}/total", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(59.98));
    }

    @Test
    @DisplayName("POST /userId/checkout - Should checkout cart")
    void checkout_WithValidUserId_ShouldCheckout() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/{userId}/checkout", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Checkout completed successfully"));

        verify(cartService).checkout(any(UUID.class));
    }

    // Saved For Later Tests

    @Test
    @DisplayName("GET /userId/saved - Should get saved items")
    void getSavedItems_WithValidUserId_ShouldReturnSavedItems() throws Exception {
        // Arrange
        List<SavedForLater> savedItems = List.of(testSavedItem);
        when(savedForLaterService.getSavedItems(any(UUID.class))).thenReturn(savedItems);

        // Act & Assert
        mockMvc.perform(get("/{userId}/saved", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].productId").value(productId.toString()));
    }

    @Test
    @DisplayName("POST /userId/saved - Should save item for later")
    void saveForLater_WithValidRequest_ShouldSaveItem() throws Exception {
        // Arrange
        SaveForLaterRequest request = SaveForLaterRequest.builder()
                .productId(productId)
                .build();

        when(savedForLaterService.saveForLater(any(UUID.class), eq(productId))).thenReturn(testSavedItem);

        // Act & Assert
        mockMvc.perform(post("/{userId}/saved", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item saved for later"));
    }

    @Test
    @DisplayName("DELETE /userId/saved/productId - Should remove saved item")
    void removeFromSaved_WithValidIds_ShouldRemoveItem() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/{userId}/saved/{productId}", userId.toString(), productId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item removed from saved items"));

        verify(savedForLaterService).removeFromSaved(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("GET /userId/saved/count - Should get saved items count")
    void getSavedItemsCount_WithValidUserId_ShouldReturnCount() throws Exception {
        // Arrange
        long expectedCount = 3L;
        when(savedForLaterService.getSavedItemCount(any(UUID.class))).thenReturn(expectedCount);

        // Act & Assert
        mockMvc.perform(get("/{userId}/saved/count", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("GET /userId/saved/productId/exists - Should check if product is saved")
    void isProductSaved_WithValidIds_ShouldReturnStatus() throws Exception {
        // Arrange
        when(savedForLaterService.isProductSaved(any(UUID.class), any(UUID.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/{userId}/saved/{productId}/exists", userId.toString(), productId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("Should handle invalid UUID format")
    void getCart_WithInvalidUUIDFormat_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/{userId}", "invalid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid UUID format")));
    }
}