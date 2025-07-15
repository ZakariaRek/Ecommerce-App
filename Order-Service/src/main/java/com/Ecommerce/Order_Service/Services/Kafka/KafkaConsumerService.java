package com.Ecommerce.Order_Service.Services.Kafka;

import com.Ecommerce.Order_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for consuming events from Kafka topics from other services
 * and processing them for the Order Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * Listen for payment confirmation events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_PAYMENT_CONFIRMED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenPaymentConfirmed(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(eventNode.path("orderId").asText());

            // Update order status to PAID
            orderService.updateOrderStatus(orderId, OrderStatus.PAID);

            log.info("Updated order status to PAID after payment confirmation. Order ID: {}", orderId);
        } catch (EntityNotFoundException e) {
            log.error("Order not found for payment confirmation event", e);
        } catch (Exception e) {
            log.error("Error processing payment confirmation event", e);
        }
    }

    /**
     * Listen for shipping update events
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_SHIPPING_UPDATE, groupId = "${spring.kafka.consumer.group-id}")
    public void listenShippingUpdate(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(eventNode.path("orderId").asText());
            String status = eventNode.path("status").asText();

            // Map shipping status to order status
            OrderStatus orderStatus;
            switch (status.toUpperCase()) {
                case "SHIPPED":
                    orderStatus = OrderStatus.SHIPPED;
                    break;
                case "DELIVERED":
                    orderStatus = OrderStatus.DELIVERED;
                    break;
                case "PROCESSING":
                    orderStatus = OrderStatus.PROCESSING;
                    break;
                default:
                    log.warn("Unknown shipping status: {}", status);
                    return;
            }

            // Update order status
            orderService.updateOrderStatus(orderId, orderStatus);

            log.info("Updated order status to {} after shipping update. Order ID: {}", orderStatus, orderId);
        } catch (EntityNotFoundException e) {
            log.error("Order not found for shipping update event", e);
        } catch (Exception e) {
            log.error("Error processing shipping update event", e);
        }
    }

    /**
     * Listen for cart checkout events to create orders
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_CART_CHECKED_OUT, groupId = "${spring.kafka.consumer.group-id}")
    public void listenCartCheckedOut(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID userId = UUID.fromString(eventNode.path("userId").asText());
            UUID cartId = UUID.fromString(eventNode.path("cartId").asText());

            // Extract shipping and billing address IDs
            // In a real implementation, you might get these from a user service or the event itself
            UUID billingAddressId = UUID.fromString(eventNode.path("billingAddressId").asText("00000000-0000-0000-0000-000000000000"));
            UUID shippingAddressId = UUID.fromString(eventNode.path("shippingAddressId").asText("00000000-0000-0000-0000-000000000000"));

            // Create new order
            Order newOrder = orderService.createOrder(userId, cartId, billingAddressId, shippingAddressId);

            // Process items from cart (this would typically be more elaborate)
            JsonNode itemsNode = eventNode.path("items");
            if (itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(UUID.fromString(itemNode.path("productId").asText()));
                    orderItem.setQuantity(itemNode.path("quantity").asInt());
                    orderItem.setPriceAtPurchase(new BigDecimal(itemNode.path("price").asText("0.0")));
                    orderItem.setDiscount(new BigDecimal(itemNode.path("discount").asText("0.0")));

                    orderService.addOrderItem(newOrder.getId(), orderItem);
                }
            }

            log.info("Created new order from cart checkout event. Order ID: {}, User ID: {}", newOrder.getId(), userId);
        } catch (Exception e) {
            log.error("Error processing cart checkout event", e);
        }
    }

    /**
     * Listen for product price changes to update orders that are still PENDING
     */
    @KafkaListener(topics = KafkaProducerConfig.TOPIC_PRODUCT_PRICE_CHANGED, groupId = "${spring.kafka.consumer.group-id}")
    public void listenProductPriceChanged(String message) {
        try {
            JsonNode eventNode = objectMapper.readTree(message);
            UUID productId = UUID.fromString(eventNode.path("productId").asText());
            BigDecimal newPrice = new BigDecimal(eventNode.path("newPrice").asText());

            // In a real implementation, you would update order items for PENDING orders
            // This is just a placeholder for the concept
            log.info("Received product price change event. Product ID: {}, New Price: {}", productId, newPrice);
        } catch (Exception e) {
            log.error("Error processing product price change event", e);
        }
    }

}