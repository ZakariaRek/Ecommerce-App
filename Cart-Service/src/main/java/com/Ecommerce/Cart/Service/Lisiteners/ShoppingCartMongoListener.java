package com.Ecommerce.Cart.Service.Lisiteners;

import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Services.Kafka.ShoppingCartKafkaService;
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

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

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
     * Called before a document is deleted
     */
    @Override
    public void onBeforeDelete(BeforeDeleteEvent<ShoppingCart> event) {
        try {
            // We don't have direct access to the document here, just the id
            // We'll need to rely on the CartService to publish removal events

            // The document id is in the event's DBObject as _id
            Object id = event.getSource().get("_id");
            log.debug("Preparing to delete shopping cart with id: {}", id);

            // We can't access the actual ShoppingCart here
            // The service layer should handle publishing removal events
        } catch (Exception e) {
            log.error("Error in shopping cart MongoDB listener before delete", e);
        }
    }

    /**
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<ShoppingCart> event) {
        try {
            // Similar to onBeforeDelete, we only have access to the document id
            // The service layer should handle publishing removal events

            Object id = event.getSource().get("_id");
            log.debug("Shopping cart with id: {} was deleted", id);
        } catch (Exception e) {
            log.error("Error in shopping cart MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new shopping cart
     */
    private void handleCartCreation(ShoppingCart cart) {
        // Device and channel info would typically come from the request context
        // For this example, we're setting default values
        String deviceInfo = "UNKNOWN_DEVICE";
        String channelType = "WEB";

        kafkaService.publishCartCreated(cart, deviceInfo, channelType);
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
        }

        if (oldItemCount > 0 && newItemCount == 0) {
            log.debug("All items removed from shopping cart: {}", cart.getId());
        }
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(ShoppingCart cart) {
        String key = getEntityKey(cart);
        entityStateMap.put(key, new EntityState(
                cart.getItems().size(),
                cart.calculateTotal(),
                cart.getUpdatedAt()
        ));
        log.debug("Stored state before shopping cart save: {}", cart.getId());
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