package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Exception.ResourceNotFoundException;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.MoveToCartRequest;
import com.Ecommerce.Cart.Service.Payload.Request.SaveForLaterRequest;
import com.Ecommerce.Cart.Service.Repositories.SavedForLaterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedForLaterService {
    private final SavedForLaterRepository savedForLaterRepository;
    private final ShoppingCartService cartService;

    /**
     * Get saved items for a user (cached)
     * This method caches List<SavedForLater>
     */
    @Cacheable(value = "savedItems", key = "#userId.toString()")
    public List<SavedForLater> getSavedItems(UUID userId) {
        log.debug("Getting saved items for userId: {}", userId);
        return savedForLaterRepository.findByUserId(userId);
    }

    /**
     * Save an item for later - EVICT cache to avoid casting issues
     * ✅ CRITICAL FIX: Use @CacheEvict instead of @CachePut to avoid casting error
     */
    @CacheEvict(value = "savedItems", key = "#userId.toString()")
    public SavedForLater saveForLater(UUID userId, UUID productId) {
        log.debug("Saving item for later: userId={}, productId={}", userId, productId);

        // Check if item is already saved to prevent duplicates
        boolean alreadySaved = savedForLaterRepository.existsByUserIdAndProductId(userId, productId);
        if (alreadySaved) {
            log.warn("Item {} already saved for user {}", productId, userId);
            throw new IllegalArgumentException("Item is already saved for later");
        }

        SavedForLater savedItem = SavedForLater.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(productId)
                .savedAt(LocalDateTime.now())
                .build();

        SavedForLater saved = savedForLaterRepository.save(savedItem);
        log.info("Successfully saved item for later: userId={}, productId={}, savedItemId={}",
                userId, productId, saved.getId());

        return saved;
    }

    /**
     * Overloaded method for SaveForLaterRequest
     */
    public SavedForLater saveForLater(UUID userId, SaveForLaterRequest request) {
        return saveForLater(userId, request.getProductId());
    }

    /**
     * Move an item to cart and remove from saved items
     */
    @Transactional
    @CacheEvict(value = "savedItems", key = "#userId.toString()")
    public ShoppingCart moveToCart(UUID userId, UUID productId, BigDecimal price) {
        log.debug("Moving saved item to cart: userId={}, productId={}, price={}", userId, productId, price);

        // Verify item exists in saved items
        if (!savedForLaterRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Saved item not found for product: " + productId);
        }

        // Add to cart first
        ShoppingCart updatedCart = cartService.addItemToCart(userId, productId, 1, price);

        // Remove from saved items
        savedForLaterRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Successfully moved item from saved to cart: userId={}, productId={}", userId, productId);

        return updatedCart;
    }

    /**
     * Overloaded method for MoveToCartRequest
     */
    @Transactional
    public ShoppingCart moveToCart(UUID userId, UUID productId, MoveToCartRequest request) {
        return moveToCart(userId, productId, request.getPrice());
    }

    /**
     * Remove an item from saved items
     */
    @CacheEvict(value = "savedItems", key = "#userId.toString()")
    public void removeFromSaved(UUID userId, UUID productId) {
        log.debug("Removing saved item: userId={}, productId={}", userId, productId);

        // Verify item exists before deleting
        if (!savedForLaterRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Saved item not found for product: " + productId);
        }

        savedForLaterRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Successfully removed saved item: userId={}, productId={}", userId, productId);
    }

    /**
     * Clear all saved items for a user
     */
    @CacheEvict(value = "savedItems", key = "#userId.toString()")
    public void clearAllSavedItems(UUID userId) {
        log.debug("Clearing all saved items for userId: {}", userId);

        long count = savedForLaterRepository.countByUserId(userId);
        if (count > 0) {
            savedForLaterRepository.deleteByUserId(userId);
            log.info("Cleared {} saved items for userId: {}", count, userId);
        }
    }

    /**
     * Get count of saved items for a user
     */
    public long getSavedItemCount(UUID userId) {
        return savedForLaterRepository.countByUserId(userId);
    }

    /**
     * Check if a specific product is saved by user
     */
    public boolean isProductSaved(UUID userId, UUID productId) {
        return savedForLaterRepository.existsByUserIdAndProductId(userId, productId);
    }
}