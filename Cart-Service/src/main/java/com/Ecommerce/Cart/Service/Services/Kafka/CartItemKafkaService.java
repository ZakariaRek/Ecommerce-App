package com.Ecommerce.Cart.Service.Services.Kafka;

import com.Ecommerce.Cart.Service.Config.KafkaProducerConfig;
import com.Ecommerce.Cart.Service.Events.CartItemEvents;
import com.Ecommerce.Cart.Service.Models.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for sending CartItem events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartItemKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish an event when a cart item is added
     */
    public void publishCartItemAdded(CartItem cartItem, UUID ProductId) {
        CartItemEvents.CartItemAddedEvent event = new CartItemEvents.CartItemAddedEvent(cartItem, ProductId);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_ITEM_ADDED, cartItem.getCartId().toString(), event);
        log.info("Published cart item added event: {}", event);
    }

    /**
     * Publish an event when a cart item is updated
     */
    public void publishCartItemUpdated(CartItem cartItem, int oldQuantity, BigDecimal oldPrice) {
        CartItemEvents.CartItemUpdatedEvent event = new CartItemEvents.CartItemUpdatedEvent(cartItem, oldQuantity, oldPrice);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_ITEM_UPDATED, cartItem.getCartId().toString(), event);
        log.info("Published cart item updated event: {}", event);
    }

    /**
     * Publish an event when a cart item is removed
     */
    public void publishCartItemRemoved(CartItem cartItem, String removalReason) {
        CartItemEvents.CartItemRemovedEvent event = new CartItemEvents.CartItemRemovedEvent(cartItem, removalReason);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_ITEM_REMOVED, cartItem.getCartId().toString(), event);
        log.info("Published cart item removed event: {}", event);
    }

    /**
     * Publish an event when a cart item quantity changes
     */
    public void publishCartItemQuantityChanged(CartItem cartItem, int oldQuantity) {
        CartItemEvents.CartItemQuantityChangedEvent event = new CartItemEvents.CartItemQuantityChangedEvent(cartItem, oldQuantity);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_ITEM_QUANTITY_CHANGED, cartItem.getCartId().toString(), event);
        log.info("Published cart item quantity changed event: {}", event);
    }

    /**
     * Publish an event when a cart item price changes
     */
    public void publishCartItemPriceChanged(CartItem cartItem, BigDecimal oldPrice, String reason) {
        CartItemEvents.CartItemPriceChangedEvent event = new CartItemEvents.CartItemPriceChangedEvent(cartItem, oldPrice, reason);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_ITEM_PRICE_CHANGED, cartItem.getCartId().toString(), event);
        log.info("Published cart item price changed event: {}", event);
    }

    /**
     * Publish an event when a cart item goes out of stock
     */
    public void publishCartItemOutOfStock(CartItem cartItem, int availableQuantity) {
        CartItemEvents.CartItemOutOfStockEvent event = new CartItemEvents.CartItemOutOfStockEvent(cartItem, availableQuantity);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_ITEM_QUANTITY_CHANGED, cartItem.getCartId().toString(), event);
        log.info("Published cart item out of stock event: {}", event);
    }


}