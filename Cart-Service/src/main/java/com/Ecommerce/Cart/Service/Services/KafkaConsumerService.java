package com.Ecommerce.Cart.Service.Services;

import com.Ecommerce.Cart.Service.Config.KafkaConfig;
import com.Ecommerce.Cart.Service.Events.CartCheckoutEvent;
import com.Ecommerce.Cart.Service.Events.CartUpdatedEvent;
import com.Ecommerce.Cart.Service.Events.ItemSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * This service demonstrates how other microservices would consume events from Kafka.
 * In a real system, these listeners would be in separate microservices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    /**
     * Listen for cart update events
     * In a real system, this might be in an Inventory Service or Analytics Service
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_CART_UPDATES, groupId = "${spring.kafka.consumer.group-id}")
    public void listenCartUpdates(CartUpdatedEvent event) {
        log.info("Received cart update event: {}", event);

        // Example of handling different actions
        switch (event.getAction()) {
            case "ADDED":
                log.info("User {} added product {} to cart", event.getUserId(), event.getProductId());
                // In a real system, you might:
                // - Update inventory reservations
                // - Send recommendations based on the added item
                // - Track for analytics
                break;

            case "REMOVED":
                log.info("User {} removed product {} from cart", event.getUserId(), event.getProductId());
                // In a real system, you might:
                // - Release inventory reservations
                // - Track abandoned items
                break;

            case "UPDATED":
                log.info("User {} updated quantity of product {} to {}",
                        event.getUserId(), event.getProductId(), event.getQuantity());
                // In a real system, you might:
                // - Adjust inventory reservations
                break;
        }
    }

    /**
     * Listen for cart checkout events
     * In a real system, this might be in an Order Service
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_CART_CHECKOUT, groupId = "${spring.kafka.consumer.group-id}")
    public void listenCartCheckout(CartCheckoutEvent event) {
        log.info("Received cart checkout event: {}", event);

        // In a real system, you might:
        // - Create a new order
        // - Process payment
        // - Update inventory
        // - Send confirmation email
        log.info("Processing order for user {}, total amount: {}",
                event.getUserId(), event.getCartTotal());
    }

    /**
     * Listen for saved item events
     * In a real system, this might be in a Recommendation Service
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_ITEM_SAVED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenItemSaved(ItemSavedEvent event) {
        log.info("Received item saved event: {}", event);

        // In a real system, you might:
        // - Track for personalized recommendations
        // - Send notifications if price drops
        // - Add to user's wishlist
        log.info("User {} saved product {} for later", event.getUserId(), event.getProductId());
    }
}