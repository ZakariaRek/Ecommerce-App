package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Exception.ResourceNotFoundException;
import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.AddItemRequest;
import com.Ecommerce.Cart.Service.Repositories.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingCartService {
    private final ShoppingCartRepository cartRepository;
//    private final KafkaProducerService kafkaProducerService;

    /**
     * Get or create a shopping cart for a user
     * Cache the result with key 'shoppingCarts::userId'
     */
    @Cacheable(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ShoppingCart newCart = ShoppingCart.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plusDays(7))
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    @Cacheable(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart getCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    /**
     * Add item to cart, update the cache, and publish event to Kafka
     */
    @CachePut(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart addItemToCart(UUID userId, UUID productId, int quantity, BigDecimal price) {
        ShoppingCart cart = getOrCreateCart(userId);

        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .addedAt(LocalDateTime.now())
                .build();

        cart.addItem(item);
        ShoppingCart updatedCart = cartRepository.save(cart);

        // Publish item added event to Kafka
//        kafkaProducerService.sendCartItemAddedEvent(updatedCart, item);

        return updatedCart;
    }

    public ShoppingCart addItemToCart(UUID userId, AddItemRequest request) {
        return addItemToCart(userId, request.getProductId(), request.getQuantity(), request.getPrice());
    }

    /**
     * Remove item from cart, update the cache, and publish event to Kafka
     */
    @CachePut(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart removeItemFromCart(UUID userId, UUID productId) {
        ShoppingCart cart = getOrCreateCart(userId);
        cart.removeItem(productId);
        ShoppingCart updatedCart = cartRepository.save(cart);

        // Publish item removed event to Kafka
//        kafkaProducerService.sendCartItemRemovedEvent(userId, cart.getId(), productId);

        return updatedCart;
    }

    /**
     * Update item quantity, update the cache, and publish event to Kafka
     */
    @CachePut(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart updateItemQuantity(UUID userId, UUID productId, int newQuantity) {
        ShoppingCart cart = getOrCreateCart(userId);
        cart.updateQuantity(productId, newQuantity);
        ShoppingCart updatedCart = cartRepository.save(cart);

        // Publish item updated event to Kafka
//        kafkaProducerService.sendCartItemUpdatedEvent(updatedCart, productId, newQuantity);

        return updatedCart;
    }

    /**
     * Calculate cart total (cached)
     */
    @Cacheable(value = "cartTotals", key = "#userId.toString()")
    public BigDecimal calculateCartTotal(UUID userId) {
        ShoppingCart cart = getOrCreateCart(userId);
        return cart.calculateTotal();
    }

    /**
     * Checkout process (invalidates cache) and publishes event to Kafka
     */
    @CacheEvict(value = {"shoppingCarts", "cartTotals"}, key = "#userId.toString()")
    public void checkout(UUID userId) {
        ShoppingCart cart = getOrCreateCart(userId);
        cart.checkout();
        cartRepository.save(cart);

        // Publish cart checkout event to Kafka
//        kafkaProducerService.sendCartCheckoutEvent(cart);
    }

    /**
     * Clean up expired carts and evict from cache
     */
    @CacheEvict(value = "shoppingCarts", allEntries = true)
    public void cleanupExpiredCarts() {
        List<ShoppingCart> expiredCarts = cartRepository.findByExpiresAtBefore(LocalDateTime.now());
        cartRepository.deleteAll(expiredCarts);
    }
}