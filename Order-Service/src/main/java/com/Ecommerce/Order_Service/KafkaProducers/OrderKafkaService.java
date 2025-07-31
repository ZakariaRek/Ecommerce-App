package com.Ecommerce.Order_Service.KafkaProducers;

import com.Ecommerce.Order_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Events.OrderEvents;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for sending Order events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    // Topic for order completion events (for loyalty service)
    public static final String TOPIC_ORDER_COMPLETED = "order-completed";

    /**
     * Publish an event when an order is created
     */
    public void publishOrderCreated(Order order) {
        try {
            OrderEvents.OrderCreatedEvent event = new OrderEvents.OrderCreatedEvent(order);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_ORDER_CREATED, order.getUserId().toString(), event);
            log.info("Published order created event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish order created event", e);
        }
    }

    /**
     * Publish an event when order status changes
     */
    public void publishOrderStatusChanged(Order order, OrderStatus oldStatus) {
        try {
            OrderEvents.OrderStatusChangedEvent event = new OrderEvents.OrderStatusChangedEvent(order, oldStatus);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_ORDER_STATUS_CHANGED, order.getUserId().toString(), event);
            log.info("Published order status changed event: {}", event);

            // Check if order is now completed/confirmed - send completion event for loyalty service
            if (isOrderCompleted(order.getStatus()) && !isOrderCompleted(oldStatus)) {
                publishOrderCompleted(order);
            }
        } catch (Exception e) {
            log.error("Failed to publish order status changed event", e);
        }
    }

    /**
     * Publish an event when an order is canceled
     */
    public void publishOrderCanceled(Order order, OrderStatus previousStatus) {
        try {
            OrderEvents.OrderCanceledEvent event = new OrderEvents.OrderCanceledEvent(order, previousStatus);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_ORDER_CANCELED, order.getUserId().toString(), event);
            log.info("Published order canceled event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish order canceled event", e);
        }
    }

    /**
     * Publish an event when an order item is added
     */
    public void publishOrderItemAdded(Order order, OrderItem item) {
        try {
            OrderEvents.OrderItemAddedEvent event = new OrderEvents.OrderItemAddedEvent(order, item);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_ORDER_ITEM_ADDED, order.getUserId().toString(), event);
            log.info("Published order item added event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish order item added event", e);
        }
    }

    /**
     * Publish an event when an order item is updated
     */
    public void publishOrderItemUpdated(Order order, OrderItem item, int oldQuantity) {
        try {
            OrderEvents.OrderItemUpdatedEvent event = new OrderEvents.OrderItemUpdatedEvent(order, item, oldQuantity);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_ORDER_ITEM_UPDATED, order.getUserId().toString(), event);
            log.info("Published order item updated event: {}", event);
        } catch (Exception e) {
            log.error("Failed to publish order item updated event", e);
        }
    }

    /**
     * Publish order completion event for loyalty service
     */
    public void publishOrderCompleted(Order order) {
        try {
            // Check if this is the user's first completed order
            boolean isFirstOrder = isFirstCompletedOrder(order.getUserId());

            OrderEvents.OrderCompletedEvent event = new OrderEvents.OrderCompletedEvent(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getItems() != null ? order.getItems().size() : 0,
                    isFirstOrder,
                    "UNKNOWN", // You can enhance this to track payment method
                    order.getStatus().toString()
            );

            kafkaTemplate.send(TOPIC_ORDER_COMPLETED, order.getUserId().toString(), event);
            log.info("📦 ORDER SERVICE: Published order completed event for loyalty service: Order ID {}, User ID {}, Amount {}",
                    order.getId(), order.getUserId(), order.getTotalAmount());
        } catch (Exception e) {
            log.error("Failed to publish order completed event", e);
        }
    }

    /**
     * Check if order status represents a completed order
     */
    private boolean isOrderCompleted(OrderStatus status) {
        return status == OrderStatus.CONFIRMED ||
                status == OrderStatus.DELIVERED ||
                status == OrderStatus.SHIPPED;
    }

    /**
     * Check if this is the user's first completed order
     */
    private boolean isFirstCompletedOrder(UUID userId) {
        try {
            // Count completed orders for this user
            long completedOrderCount = orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .filter(order -> isOrderCompleted(order.getStatus()))
                    .count();

            return completedOrderCount <= 1; // Including current order
        } catch (Exception e) {
            log.error("Error checking if first order for user {}", userId, e);
            return false;
        }
    }
}