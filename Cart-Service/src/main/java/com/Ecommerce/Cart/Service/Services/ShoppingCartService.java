package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Exception.ResourceNotFoundException;
import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.AddItemRequest;
import com.Ecommerce.Cart.Service.Payload.Request.BulkUpdateItem;
import com.Ecommerce.Cart.Service.Payload.Request.BulkUpdateRequest;
import com.Ecommerce.Cart.Service.Repositories.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingCartService {
    private final ShoppingCartRepository cartRepository;

    /**
     * ✅ FIXED: Enhanced getOrCreateCart with better logging and cache handling
     */
    @Cacheable(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart getOrCreateCart(UUID userId) {
        log.debug("Getting or creating cart for userId: {}", userId);

        Optional<ShoppingCart> existingCart = cartRepository.findByUserId(userId);

        if (existingCart.isPresent()) {
            ShoppingCart cart = existingCart.get();
            log.debug("Found existing cart: ID={}, Items={}, Total={}",
                    cart.getId(),
                    cart.getItems() != null ? cart.getItems().size() : 0,
                    cart.calculateTotal());

            // ✅ Ensure items list is initialized
            if (cart.getItems() == null) {
                log.warn("Cart items list was null, initializing empty list for cart: {}", cart.getId());
                cart.setItems(List.of());
            }

            return cart;
        } else {
            log.info("No existing cart found for userId: {}. Creating new cart.", userId);

            // ✅ DEBUGGING: Check if cart exists with different UUID format
            debugCartSearch(userId);

            ShoppingCart newCart = ShoppingCart.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            ShoppingCart savedCart = cartRepository.save(newCart);
            log.info("Created new cart: ID={} for userId: {}", savedCart.getId(), userId);

            return savedCart;
        }
    }

    /**
     * ✅ NEW: Get cart without creating (for debugging)
     */
    public Optional<ShoppingCart> findCartByUserId(UUID userId) {
        log.debug("Finding cart for userId: {}", userId);
        return cartRepository.findByUserId(userId);
    }

    /**
     * ✅ DEBUGGING METHOD: Search for cart with different UUID formats
     */
    private void debugCartSearch(UUID userId) {
        try {
            log.debug("Debugging cart search for UUID: {}", userId);

            // Check all carts to see what exists
            List<ShoppingCart> allCarts = cartRepository.findAll();
            log.debug("Total carts in database: {}", allCarts.size());

            // Log first few carts for comparison
            allCarts.stream().limit(5).forEach(cart -> {
                log.debug("Existing cart: userId='{}', id='{}', items={}",
                        cart.getUserId(), cart.getId(),
                        cart.getItems() != null ? cart.getItems().size() : 0);

                // Check for partial matches
                String requestedStr = userId.toString().replace("-", "");
                String existingStr = cart.getUserId().toString().replace("-", "");

                if (requestedStr.length() >= 24 && existingStr.length() >= 24) {
                    String requestedPrefix = requestedStr.substring(0, 24);
                    String existingPrefix = existingStr.substring(0, 24);

                    if (requestedPrefix.equals(existingPrefix)) {
                        log.warn("FOUND POTENTIAL MATCH! Requested: '{}', Existing: '{}', CartId: '{}'",
                                userId, cart.getUserId(), cart.getId());
                    }
                }
            });

        } catch (Exception e) {
            log.error("Error during cart debugging", e);
        }
    }

    /**
     * ✅ FIXED: Get cart with proper error handling
     */
    @Cacheable(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart getCart(UUID userId) {
        log.debug("Getting cart for userId: {}", userId);

        Optional<ShoppingCart> cart = cartRepository.findByUserId(userId);
        if (cart.isPresent()) {
            log.debug("Found cart: ID={}, Items={}",
                    cart.get().getId(),
                    cart.get().getItems() != null ? cart.get().getItems().size() : 0);
            return cart.get();
        } else {
            log.warn("Cart not found for userId: {}", userId);
            debugCartSearch(userId);
            throw new ResourceNotFoundException("Cart not found for user: " + userId);
        }
    }

    /**
     * ✅ Enhanced cache eviction and logging
     */
    @CachePut(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart addItemToCart(UUID userId, UUID productId, int quantity, BigDecimal price) {
        log.debug("Adding item to cart: userId={}, productId={}, quantity={}, price={}",
                userId, productId, quantity, price);

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

        log.info("Added item to cart: cartId={}, total items={}, new total={}",
                updatedCart.getId(),
                updatedCart.getItems().size(),
                updatedCart.calculateTotal());

        return updatedCart;
    }

    public ShoppingCart addItemToCart(UUID userId, AddItemRequest request) {
        return addItemToCart(userId, request.getProductId(), request.getQuantity(), request.getPrice());
    }

    @CachePut(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart removeItemFromCart(UUID userId, UUID productId) {
        ShoppingCart cart = getOrCreateCart(userId);
        cart.removeItem(productId);
        ShoppingCart updatedCart = cartRepository.save(cart);

        log.info("Removed item from cart: cartId={}, remaining items={}",
                updatedCart.getId(), updatedCart.getItems().size());

        return updatedCart;
    }

    @CachePut(value = "shoppingCarts", key = "#userId.toString()")
    public ShoppingCart updateItemQuantity(UUID userId, UUID productId, int newQuantity) {
        ShoppingCart cart = getOrCreateCart(userId);
        cart.updateQuantity(productId, newQuantity);
        ShoppingCart updatedCart = cartRepository.save(cart);

        log.info("Updated item quantity in cart: cartId={}, productId={}, newQuantity={}",
                updatedCart.getId(), productId, newQuantity);

        return updatedCart;
    }

    /**
     * ✅ FIXED: Calculate total with better cache handling
     */
    @Cacheable(value = "cartTotals", key = "#userId.toString()")
    public BigDecimal calculateCartTotal(UUID userId) {
        log.debug("Calculating cart total for userId: {}", userId);

        // ✅ Use findCartByUserId to avoid creating a new cart just for total calculation
        Optional<ShoppingCart> cartOpt = findCartByUserId(userId);
        if (cartOpt.isPresent()) {
            BigDecimal total = cartOpt.get().calculateTotal();
            log.debug("Cart total for userId {}: {}", userId, total);
            return total;
        } else {
            log.debug("No cart found for userId: {}, returning zero total", userId);
            return BigDecimal.ZERO;
        }
    }

    @CacheEvict(value = {"shoppingCarts", "cartTotals"}, key = "#userId.toString()")
    public void checkout(UUID userId) {
        ShoppingCart cart = getOrCreateCart(userId);
        cart.checkout();
        cartRepository.save(cart);

        log.info("Checked out cart: cartId={}, userId={}", cart.getId(), userId);
    }

    @CacheEvict(value = "shoppingCarts", allEntries = true)
    public void cleanupExpiredCarts() {
        List<ShoppingCart> expiredCarts = cartRepository.findByExpiresAtBefore(LocalDateTime.now());
        cartRepository.deleteAll(expiredCarts);

        log.info("Cleaned up {} expired carts", expiredCarts.size());
    }

    @CachePut(value = "shoppingCarts", key = "#userId.toString()")
    @Transactional
    public ShoppingCart bulkUpdateCart(UUID userId, BulkUpdateRequest request) {
        ShoppingCart cart = getOrCreateCart(userId);

        for (BulkUpdateItem updateItem : request.getItems()) {
            switch (updateItem.getOperation()) {
                case ADD:
                    addItemToExistingCart(cart, updateItem);
                    break;
                case UPDATE:
                    updateItemInCart(cart, updateItem);
                    break;
                case REMOVE:
                    cart.removeItem(updateItem.getProductId());
                    break;
            }
        }

        ShoppingCart updatedCart = cartRepository.save(cart);
        log.info("Bulk updated cart: cartId={}, operations={}",
                updatedCart.getId(), request.getItems().size());

        return updatedCart;
    }

    private void addItemToExistingCart(ShoppingCart cart, BulkUpdateItem updateItem) {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID())
                .productId(updateItem.getProductId())
                .quantity(updateItem.getQuantity())
                .price(updateItem.getPrice())
                .addedAt(LocalDateTime.now())
                .build();
        cart.addItem(item);
    }

    private void updateItemInCart(ShoppingCart cart, BulkUpdateItem updateItem) {
        cart.updateQuantity(updateItem.getProductId(), updateItem.getQuantity());
    }
}