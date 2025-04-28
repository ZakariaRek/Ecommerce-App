package com.Ecommerce.Cart.Service.Lisiteners;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Services.Kafka.CartItemKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
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

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

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
     * Called before a document is deleted
     */
    @Override
    public void onBeforeDelete(BeforeDeleteEvent<CartItem> event) {
        try {
            // We don't have direct access to the document here, just the id
            // We'll need to rely on the CartItemService to publish removal events

            // The document id is in the event's DBObject as _id
            Object id = event.getSource().get("_id");
            log.debug("Preparing to delete cart item with id: {}", id);

            // We can't access the actual CartItem here
            // The service layer should handle publishing removal events
        } catch (Exception e) {
            log.error("Error in cart item MongoDB listener before delete", e);
        }
    }

    /**
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<CartItem> event) {
        try {
            // Similar to onBeforeDelete, we only have access to the document id
            // The service layer should handle publishing removal events

            Object id = event.getSource().get("_id");
            log.debug("Cart item with id: {} was deleted", id);
        } catch (Exception e) {
            log.error("Error in cart item MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new cart item
     */
    private void handleCartItemCreation(CartItem cartItem) {
        kafkaService.publishCartItemAdded(cartItem, null);
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
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(CartItem cartItem) {
        String key = getEntityKey(cartItem);
        entityStateMap.put(key, new EntityState(cartItem.getQuantity(), cartItem.getPrice()));
        log.debug("Stored state before cart item save: {}", cartItem.getId());
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