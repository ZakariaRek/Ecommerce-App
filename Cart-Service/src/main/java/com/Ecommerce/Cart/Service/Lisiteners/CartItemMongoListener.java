package com.Ecommerce.Cart.Service.Lisiteners;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Services.Kafka.CartItemKafkaService;
import com.Ecommerce.Cart.Service.Services.CartItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for CartItem documents to automatically publish events to Kafka
 * when cart items are created, updated, or removed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartItemMongoListener extends AbstractMongoEventListener<CartItem> {

    private final CartItemKafkaService kafkaService;
    private final CartItemService cartItemService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    // Store items before deletion to use in after-delete events
    private static final Map<String, CartItem> preDeleteItemMap = new ConcurrentHashMap<>();

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<CartItem> event) {
        CartItem cartItem = event.getSource();
        String key = getEntityKey(cartItem);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update
                handleCartItemUpdate(cartItem, oldState);
            } else {
                // This is a new item
                handleCartItemCreation(cartItem);
            }
        } catch (Exception e) {
            log.error("Error in cart item MongoDB listener after save", e);
        }
    }



    /**
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<CartItem> event) {
        try {
            // Extract the ID from the deleted document
            Object id = event.getSource().get("_id");
            if (id != null) {
                String documentId = id.toString();
                log.debug("Cart item with id: {} was deleted", documentId);

                // Look for the stored CartItem from the beforeDelete event
                String key = "CartItem:" + documentId;
                CartItem deletedItem = preDeleteItemMap.remove(key);

                if (deletedItem != null) {
                    // Now we have the complete item that was deleted, publish the event
                    kafkaService.publishCartItemRemoved(deletedItem, "user_removed");
                    log.debug("Published Kafka event for removed cart item: {}", documentId);
                } else {
                    log.warn("Could not find pre-delete state for cart item with id: {}", documentId);
                }
            }
        } catch (Exception e) {
            log.error("Error in cart item MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new cart item
     */
    private void handleCartItemCreation(CartItem cartItem) {
        // Fetch product ID if needed (could be from a product service)
        UUID ProductID = cartItem.getProductId();
        kafkaService.publishCartItemAdded(cartItem, ProductID);
        log.debug("MongoDB listener triggered for cart item creation: {}", cartItem.getId());
    }

    /**
     * Handle updates to an existing cart item
     */
    private void handleCartItemUpdate(CartItem cartItem, EntityState oldState) {
        // Check what changed
        if (oldState.quantity != cartItem.getQuantity()) {
            kafkaService.publishCartItemQuantityChanged(cartItem, oldState.quantity);
            log.debug("MongoDB listener triggered for cart item quantity change: {}", cartItem.getId());
        }

        if (!oldState.price.equals(cartItem.getPrice())) {
            kafkaService.publishCartItemPriceChanged(cartItem, oldState.price, "price_update");
            log.debug("MongoDB listener triggered for cart item price change: {}", cartItem.getId());
        }

        // Always send a generic update event
        kafkaService.publishCartItemUpdated(cartItem, oldState.quantity, oldState.price);
        log.debug("MongoDB listener triggered for cart item update: {}", cartItem.getId());
    }


    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(CartItem cartItem) {
        return "CartItem:" + cartItem.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final int quantity;
        private final BigDecimal price;

        public EntityState(int quantity, BigDecimal price) {
            this.quantity = quantity;
            this.price = price;
        }
    }
}