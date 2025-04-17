//package com.Ecommerce.Cart.Service.Services;
//
//import com.Ecommerce.Cart.Service.Config.KafkaConfig;
//import com.Ecommerce.Cart.Service.Events.CartCheckoutEvent;
//import com.Ecommerce.Cart.Service.Events.CartUpdatedEvent;
//import com.Ecommerce.Cart.Service.Events.ItemSavedEvent;
//import com.Ecommerce.Cart.Service.Models.CartItem;
//import com.Ecommerce.Cart.Service.Models.ShoppingCart;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.SendResult;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class KafkaProducerService {
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    /**
//     * Send cart updated event when an item is added to the cart
//     */
//    public void sendCartItemAddedEvent(ShoppingCart cart, CartItem item) {
//        CartUpdatedEvent event = CartUpdatedEvent.builder()
//                .userId(cart.getUserId())
//                .cartId(cart.getId())
//                .action("ADDED")
//                .productId(item.getProductId())
//                .quantity(item.getQuantity())
//                .price(item.getPrice())
//                .cartTotal(cart.calculateTotal())
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        sendCartUpdatedEvent(event);
//    }
//
//    /**
//     * Send cart updated event when an item is removed from the cart
//     */
//    public void sendCartItemRemovedEvent(UUID userId, UUID cartId, UUID productId) {
//        CartUpdatedEvent event = CartUpdatedEvent.builder()
//                .userId(userId)
//                .cartId(cartId)
//                .action("REMOVED")
//                .productId(productId)
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        sendCartUpdatedEvent(event);
//    }
//
//    /**
//     * Send cart updated event when an item quantity is updated
//     */
//    public void sendCartItemUpdatedEvent(ShoppingCart cart, UUID productId, int newQuantity) {
//        CartItem item = cart.getItems().stream()
//                .filter(i -> i.getProductId().equals(productId))
//                .findFirst()
//                .orElse(null);
//
//        if (item != null) {
//            CartUpdatedEvent event = CartUpdatedEvent.builder()
//                    .userId(cart.getUserId())
//                    .cartId(cart.getId())
//                    .action("UPDATED")
//                    .productId(productId)
//                    .quantity(newQuantity)
//                    .price(item.getPrice())
//                    .cartTotal(cart.calculateTotal())
//                    .timestamp(LocalDateTime.now())
//                    .build();
//
//            sendCartUpdatedEvent(event);
//        }
//    }
//
//    /**
//     * Send checkout event when a cart is checked out
//     */
//    public void sendCartCheckoutEvent(ShoppingCart cart) {
//        CartCheckoutEvent event = CartCheckoutEvent.builder()
//                .userId(cart.getUserId())
//                .cartId(cart.getId())
//                .cartTotal(cart.calculateTotal())
//                .itemCount(cart.getItems().size())
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
//                KafkaConfig.TOPIC_CART_CHECKOUT,
//                cart.getUserId().toString(),
//                event);
//
//        future.whenComplete((result, ex) -> {
//            if (ex == null) {
//                log.info("Cart checkout event sent successfully for user: {}", cart.getUserId());
//            } else {
//                log.error("Unable to send cart checkout event for user: {}", cart.getUserId(), ex);
//            }
//        });
//    }
//
//    /**
//     * Send item saved event when an item is saved for later
//     */
//    public void sendItemSavedEvent(UUID userId, UUID productId) {
//        ItemSavedEvent event = ItemSavedEvent.builder()
//                .userId(userId)
//                .productId(productId)
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
//                KafkaConfig.TOPIC_ITEM_SAVED,
//                userId.toString(),
//                event);
//
//        future.whenComplete((result, ex) -> {
//            if (ex == null) {
//                log.info("Item saved event sent successfully for user: {}", userId);
//            } else {
//                log.error("Unable to send item saved event for user: {}", userId, ex);
//            }
//        });
//    }
//
//    /**
//     * Private helper method to send cart updated event
//     */
//    private void sendCartUpdatedEvent(CartUpdatedEvent event) {
//        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
//                KafkaConfig.TOPIC_CART_UPDATES,
//                event.getUserId().toString(),
//                event);
//
//        future.whenComplete((result, ex) -> {
//            if (ex == null) {
//                log.info("Cart updated event sent successfully for user: {}", event.getUserId());
//            } else {
//                log.error("Unable to send cart updated event for user: {}", event.getUserId(), ex);
//            }
//        });
//    }
//}