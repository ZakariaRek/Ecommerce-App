package com.Ecommerce.Cart.Service.Lisiteners;

import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Services.Kafka.ShoppingCartKafkaService;
import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for ShoppingCart documents to automatically publish events to Kafka
 * when shopping carts are created, updated, or removed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingCartMongoListener extends AbstractMongoEventListener<ShoppingCart> {

    private final ShoppingCartKafkaService kafkaService;
    private final ShoppingCartService shoppingCartService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    // Store carts before deletion to use in after-delete events
    private static final Map<String, ShoppingCart> preDeleteCartMap = new ConcurrentHashMap<>();

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<ShoppingCart> event) {
        ShoppingCart cart = event.getSource();
        String key = getEntityKey(cart);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update
                handleCartUpdate(cart, oldState);
            } else {
                // This is a new cart
                handleCartCreation(cart);
            }
        } catch (Exception e) {
            log.error("Error in shopping cart MongoDB listener after save", e);
        }
    }



    /**
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<ShoppingCart> event) {
        try {
            // Extract the ID from the deleted document
            Object id = event.getSource().get("_id");
            if (id != null) {
                String documentId = id.toString();
                log.debug("Shopping cart with id: {} was deleted", documentId);

                // Look for the stored ShoppingCart from the beforeDelete event
                String key = "ShoppingCart:" + documentId;
                ShoppingCart deletedCart = preDeleteCartMap.remove(key);

                if (deletedCart != null) {
                    // Now we have the complete cart that was deleted, publish the event
                    kafkaService.publishCartDeleted(deletedCart, "user_deleted");
                    log.debug("Published Kafka event for removed shopping cart: {}", documentId);
                } else {
                    log.warn("Could not find pre-delete state for shopping cart with id: {}", documentId);
                }
            }
        } catch (Exception e) {
            log.error("Error in shopping cart MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new shopping cart
     */
    private void handleCartCreation(ShoppingCart cart) {


        kafkaService.publishCartCreated(cart);
        log.debug("MongoDB listener triggered for shopping cart creation: {}", cart.getId());
    }

    /**
     * Handle updates to an existing shopping cart
     */
    private void handleCartUpdate(ShoppingCart cart, EntityState oldState) {
        // Always send a generic update event
        kafkaService.publishCartUpdated(cart);
        log.debug("MongoDB listener triggered for shopping cart update: {}", cart.getId());

        // Check specific conditions that might trigger additional events
        BigDecimal oldTotal = oldState.totalAmount;
        BigDecimal newTotal = cart.calculateTotal();

        if (!oldTotal.equals(newTotal)) {
            log.debug("Shopping cart total changed from {} to {}", oldTotal, newTotal);
        }

        int oldItemCount = oldState.itemCount;
        int newItemCount = cart.getItems().size();

        if (oldItemCount == 0 && newItemCount > 0) {
            log.debug("First item added to shopping cart: {}", cart.getId());
            // Could publish a special "first item added" event if needed
        }

        if (oldItemCount > 0 && newItemCount == 0) {
            log.debug("All items removed from shopping cart: {}", cart.getId());
            // Could publish a special "cart emptied" event if needed
        }

        // Check for cart abandonment potential
        LocalDateTime lastUpdated = oldState.lastUpdatedAt;
        LocalDateTime now = LocalDateTime.now();
        long hoursSinceLastUpdate = java.time.Duration.between(lastUpdated, now).toHours();

        // If cart hasn't been updated in 24 hours and has items, consider it abandoned
        if (oldItemCount > 0 && hoursSinceLastUpdate >= 24) {
            kafkaService.publishCartAbandoned(cart, now);
            log.debug("Published cart abandoned event for cart: {}", cart.getId());
        }
    }



    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(ShoppingCart cart) {
        return "ShoppingCart:" + cart.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final int itemCount;
        private final BigDecimal totalAmount;
        private final LocalDateTime lastUpdatedAt;

        public EntityState(int itemCount, BigDecimal totalAmount, LocalDateTime lastUpdatedAt) {
            this.itemCount = itemCount;
            this.totalAmount = totalAmount;
            this.lastUpdatedAt = lastUpdatedAt;
        }
    }
}