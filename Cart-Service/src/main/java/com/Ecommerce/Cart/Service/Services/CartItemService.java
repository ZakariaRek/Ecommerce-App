package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Repositories.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing cart items
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Add a new item to cart
     */
    public CartItem addItemToCart(UUID cartId, UUID productId, int quantity, BigDecimal price) {
        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cartId, productId);

        if (existingItem.isPresent()) {
            // Update quantity of existing item
            CartItem item = existingItem.get();
            item.updateQuantity(item.getQuantity() + quantity);
            return cartItemRepository.save(item);
        } else {
            // Create new cart item
            CartItem newItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .cartId(cartId)
                    .productId(productId)
                    .quantity(quantity)
                    .price(price)
                    .addedAt(LocalDateTime.now())
                    .build();

            return cartItemRepository.save(newItem);
        }
    }

    /**
     * Update cart item quantity
     */
    public CartItem updateQuantity(UUID cartItemId, int newQuantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        cartItem.updateQuantity(newQuantity);
        return cartItemRepository.save(cartItem);
    }

    /**
     * Remove item from cart
     */
    public void removeItem(UUID cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    /**
     * Get all items in a cart
     */
    public List<CartItem> getCartItems(UUID cartId) {
        return cartItemRepository.findByCartId(cartId);
    }

    /**
     * Get specific cart item
     */
    public Optional<CartItem> getCartItem(UUID cartItemId) {
        return cartItemRepository.findById(cartItemId);
    }

    /**
     * Update price for all cart items with a specific product
     * Used when the product's price changes in the catalog
     */
    public void updatePriceForProduct(UUID productId, BigDecimal newPrice) {
        Query query = new Query(Criteria.where("productId").is(productId));
        Update update = new Update().set("price", newPrice);

        mongoTemplate.updateMulti(query, update, CartItem.class);
        log.info("Updated price to {} for product ID: {}", newPrice, productId);
    }

    /**
     * Remove a product from all carts
     * Used when product is deleted or out of stock
     */
    public void removeProductFromAllCarts(UUID productId) {
        Query query = new Query(Criteria.where("productId").is(productId));
        mongoTemplate.remove(query, CartItem.class);
        log.info("Removed all cart items with product ID: {}", productId);
    }

    /**
     * Adjust quantities for a product if cart quantity exceeds available inventory
     */
    public void adjustQuantitiesForProduct(UUID productId, int availableQuantity) {
        List<CartItem> cartItems = cartItemRepository.findByProductId(productId);

        for (CartItem item : cartItems) {
            if (item.getQuantity() > availableQuantity) {
                if (availableQuantity > 0) {
                    // Reduce quantity
                    item.updateQuantity(availableQuantity);
                    cartItemRepository.save(item);
                    log.info("Adjusted quantity to {} for cart item: {}",
                            availableQuantity, item.getId());
                } else {
                    // Remove item if no stock available
                    cartItemRepository.delete(item);
                    log.info("Removed out-of-stock cart item: {}", item.getId());
                }
            }
        }
    }
}