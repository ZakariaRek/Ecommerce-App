package com.Ecommerce.Cart.Service.Services;


import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.*;
import com.Ecommerce.Cart.Service.Payload.Response.CartValidationItem;
import com.Ecommerce.Cart.Service.Payload.Response.CartValidationResponse;
import com.Ecommerce.Cart.Service.Repositories.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartSyncService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemService cartItemService;
    // Add ProductService injection for price validation

    @Transactional
    public ShoppingCart mergeWithLocalStorage(ShoppingCart serverCart,
                                              CartSyncRequest syncRequest) {
        log.info("Merging localStorage cart with server cart for user: {}", serverCart.getUserId());

        List<LocalStorageItem> validItems = validateLocalStorageItems(syncRequest.getItems());

        for (LocalStorageItem localItem : validItems) {
            CartItem existingServerItem = findExistingItemInCart(serverCart, localItem.getProductId());

            if (existingServerItem != null) {
                handleItemConflict(existingServerItem, localItem, syncRequest.getConflictStrategy());
            } else {
                addLocalItemToServerCart(serverCart, localItem);
            }
        }

        ShoppingCart savedCart = cartRepository.save(serverCart);
        log.info("Successfully merged cart for user: {}, final item count: {}",
                serverCart.getUserId(), savedCart.getItems().size());

        return savedCart;
    }

    public CartValidationResponse validateGuestCart(GuestCartRequest guestCart) {
        List<CartValidationItem> validationResults = new ArrayList<>();
        BigDecimal totalPriceChange = BigDecimal.ZERO;

        for (GuestCartItem item : guestCart.getItems()) {
            CartValidationItem validation = validateGuestItem(item);
            validationResults.add(validation);

            if (validation.isPriceChanged()) {
                BigDecimal priceDiff = validation.getCurrentPrice().subtract(item.getPrice())
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                totalPriceChange = totalPriceChange.add(priceDiff);
            }
        }

        return CartValidationResponse.builder()
                .items(validationResults)
                .totalPriceChange(totalPriceChange)
                .hasChanges(validationResults.stream().anyMatch(CartValidationItem::hasChanges))
                .build();
    }

    private void handleItemConflict(CartItem serverItem, LocalStorageItem localItem,
                                    ConflictResolutionStrategy strategy) {
        switch (strategy) {
            case SUM_QUANTITIES:
                int totalQuantity = serverItem.getQuantity() + localItem.getQuantity();
                serverItem.updateQuantity(totalQuantity);
                log.debug("Merged quantities: {} + {} = {}",
                        serverItem.getQuantity(), localItem.getQuantity(), totalQuantity);
                break;

            case KEEP_LATEST:
                if (localItem.getUpdatedAt().isAfter(serverItem.getAddedAt())) {
                    serverItem.updateQuantity(localItem.getQuantity());
                    // Validate and update price if needed
                    validateAndUpdatePrice(serverItem, localItem.getPrice());
                }
                break;

            case KEEP_SERVER:
                log.debug("Keeping server data for product: {}", localItem.getProductId());
                break;

            case KEEP_LOCAL:
                serverItem.updateQuantity(localItem.getQuantity());
                validateAndUpdatePrice(serverItem, localItem.getPrice());
                break;

            default:
                serverItem.updateQuantity(serverItem.getQuantity() + localItem.getQuantity());
        }
    }

    private CartItem findExistingItemInCart(ShoppingCart cart, UUID productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void addLocalItemToServerCart(ShoppingCart serverCart, LocalStorageItem localItem) {
        // Validate product and price - you'll need to implement this
        // ProductValidationResult validation = productService.validateProduct(
        //     localItem.getProductId(), localItem.getPrice());

        CartItem newItem = CartItem.builder()
                .id(UUID.randomUUID())
                .productId(localItem.getProductId())
                .quantity(localItem.getQuantity())
                .price(localItem.getPrice()) // Use validated price in real implementation
                .addedAt(LocalDateTime.now())
                .build();

        serverCart.addItem(newItem);
        log.debug("Added localStorage item to server cart: product={}, quantity={}",
                localItem.getProductId(), localItem.getQuantity());
    }

    private List<LocalStorageItem> validateLocalStorageItems(List<LocalStorageItem> items) {
        // Filter out invalid items, validate products exist, etc.
        return items.stream()
                .filter(item -> item.getProductId() != null && item.getQuantity() > 0)
                .collect(Collectors.toList());
    }

    private CartValidationItem validateGuestItem(GuestCartItem item) {
        // Implement price and availability validation
        // This is a simplified version - you'd integrate with your product service
        return CartValidationItem.builder()
                .productId(item.getProductId())
                .originalPrice(item.getPrice())
                .currentPrice(item.getPrice()) // Would get from product service
                .priceChanged(false)
                .availabilityChanged(false)
                .inStock(true)
                .validationMessage("Valid")
                .build();
    }

    private void validateAndUpdatePrice(CartItem item, BigDecimal newPrice) {
        // Implement price validation logic
        // For now, just update the price
        item.setPrice(newPrice);
    }
}
