package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Exception.ResourceNotFoundException;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.MoveToCartRequest;
import com.Ecommerce.Cart.Service.Payload.Request.SaveForLaterRequest;
import com.Ecommerce.Cart.Service.Repositories.SavedForLaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedForLaterService {
    private final SavedForLaterRepository savedForLaterRepository;
    private final ShoppingCartService cartService;

    /**
     * Get saved items for a user (cached)
     */
    @Cacheable(value = "savedItems", key = "#userId.toString()")
    public List<SavedForLater> getSavedItems(UUID userId) {
        return savedForLaterRepository.findByUserId(userId);
    }

    /**
     * Save an item for later and update the cache
     */
    @CachePut(value = "savedItems", key = "#userId.toString()")
    public SavedForLater saveForLater(UUID userId, UUID productId) {
        SavedForLater savedItem = SavedForLater.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(productId)
                .savedAt(LocalDateTime.now())
                .build();

        SavedForLater saved = savedForLaterRepository.save(savedItem);

        // Since we're returning a single item but caching a list,
        // we need to refresh the cache with the complete list
        List<SavedForLater> updatedList = savedForLaterRepository.findByUserId(userId);
        return saved;
    }

    public SavedForLater saveForLater(UUID userId, SaveForLaterRequest request) {
        return saveForLater(userId, request.getProductId());
    }

    /**
     * Move an item to cart and update both caches
     */
    @Transactional
    @CacheEvict(value = "savedItems", key = "#userId.toString()")
    public ShoppingCart moveToCart(UUID userId, UUID productId, BigDecimal price) {
        // Verify item exists in saved items
        boolean exists = savedForLaterRepository.findByUserId(userId).stream()
                .anyMatch(item -> item.getProductId().equals(productId));

        if (!exists) {
            throw new ResourceNotFoundException("Saved item not found for product: " + productId);
        }

        // Add to cart (this will update the shopping cart cache)
        ShoppingCart updatedCart = cartService.addItemToCart(userId, productId, 1, price);

        // Remove from saved items
        savedForLaterRepository.deleteByUserIdAndProductId(userId, productId);

        return updatedCart;
    }

    @Transactional
    public ShoppingCart moveToCart(UUID userId, UUID productId, MoveToCartRequest request) {
        return moveToCart(userId, productId, request.getPrice());
    }

    /**
     * Remove an item from saved items and update the cache
     */
    @CacheEvict(value = "savedItems", key = "#userId.toString()")
    public void removeFromSaved(UUID userId, UUID productId) {
        // Verify item exists before deleting
        boolean exists = savedForLaterRepository.findByUserId(userId).stream()
                .anyMatch(item -> item.getProductId().equals(productId));

        if (!exists) {
            throw new ResourceNotFoundException("Saved item not found for product: " + productId);
        }

        savedForLaterRepository.deleteByUserIdAndProductId(userId, productId);
    }
}