package com.Ecommerce.Cart.Service.Services.Kafka;

import com.Ecommerce.Cart.Service.Config.KafkaProducerConfig;
import com.Ecommerce.Cart.Service.Events.ShoppingCartEvents;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for sending ShoppingCart events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShoppingCartKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when a shopping cart is created
     */
    public void publishCartCreated(ShoppingCart cart, String deviceInfo, String channelType) {
        ShoppingCartEvents.CartCreatedEvent event = new ShoppingCartEvents.CartCreatedEvent(cart, deviceInfo, channelType);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_CREATED, cart.getUserId().toString(), event);
        log.info("Published cart created event: {}", event);
    }

    /**
     * Publish an event when a shopping cart is updated
     */
    public void publishCartUpdated(ShoppingCart cart) {
        ShoppingCartEvents.CartUpdatedEvent event = new ShoppingCartEvents.CartUpdatedEvent(cart);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_UPDATED, cart.getUserId().toString(), event);
        log.info("Published cart updated event: {}", event);
    }

    /**
     * Publish an event when a shopping cart is deleted
     */
    public void publishCartDeleted(ShoppingCart cart, String deletionReason) {
        ShoppingCartEvents.CartDeletedEvent event = new ShoppingCartEvents.CartDeletedEvent(cart, deletionReason);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_DELETED, cart.getUserId().toString(), event);
        log.info("Published cart deleted event: {}", event);
    }

    /**
     * Publish an event when a shopping cart is abandoned
     */
    public void publishCartAbandoned(ShoppingCart cart, LocalDateTime abandonedAt) {
        ShoppingCartEvents.CartAbandonedEvent event = new ShoppingCartEvents.CartAbandonedEvent(cart, abandonedAt);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_ABANDONED, cart.getUserId().toString(), event);
        log.info("Published cart abandoned event: {}", event);
    }

    /**
     * Publish an event when an abandoned shopping cart is recovered
     */
    public void publishCartRecovered(ShoppingCart cart, LocalDateTime abandonedAt, String recoveryMethod) {
        ShoppingCartEvents.CartRecoveredEvent event = new ShoppingCartEvents.CartRecoveredEvent(cart, abandonedAt, recoveryMethod);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_RECOVERED, cart.getUserId().toString(), event);
        log.info("Published cart recovered event: {}", event);
    }

    /**
     * Publish an event when a shopping cart is checked out
     */
    public void publishCartCheckedOut(ShoppingCart cart, UUID orderId, BigDecimal tax,
                                      BigDecimal shipping, BigDecimal discount, String paymentMethod) {
        ShoppingCartEvents.CartCheckedOutEvent event = new ShoppingCartEvents.CartCheckedOutEvent(
                cart, orderId, tax, shipping, discount, paymentMethod);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_CART_CHECKED_OUT, cart.getUserId().toString(), event);
        log.info("Published cart checked out event: {}", event);
    }

    /**
     * Publish an event when a coupon is applied to a cart
     */
    public void publishCouponApplied(ShoppingCart cart, String couponCode,
                                     BigDecimal discountAmount, BigDecimal originalTotal) {
        ShoppingCartEvents.CouponAppliedEvent event = new ShoppingCartEvents.CouponAppliedEvent(
                cart, couponCode, discountAmount, originalTotal);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_COUPON_APPLIED, cart.getUserId().toString(), event);
        log.info("Published coupon applied event: {}", event);
    }

    /**
     * Publish an event when a coupon is removed from a cart
     */
    public void publishCouponRemoved(ShoppingCart cart, String couponCode, BigDecimal discountAmount) {
        ShoppingCartEvents.CouponRemovedEvent event = new ShoppingCartEvents.CouponRemovedEvent(
                cart, couponCode, discountAmount);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_COUPON_REMOVED, cart.getUserId().toString(), event);
        log.info("Published coupon removed event: {}", event);
    }
}