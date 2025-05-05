package com.Ecommerce.Order_Service.Listeners;

import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Services.Kafka.OrderKafkaService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JPA Entity Listener for OrderItem entities to automatically publish events to Kafka
 * when order items are created or updated
 */
@Component
@Slf4j
public class OrderItemEntityListener {

    // Need to use static fields because JPA entity listeners are instantiated by JPA, not Spring
    private static OrderKafkaService kafkaService;

    // Store pre-change state for events
    private static final Map<UUID, Integer> entityStateMap = new ConcurrentHashMap<>();

    @Autowired
    public void setKafkaService(OrderKafkaService kafkaService) {
        OrderItemEntityListener.kafkaService = kafkaService;
    }

    /**
     * Called before an order item is updated
     */
    @PreUpdate
    public void preUpdate(OrderItem orderItem) {
        try {
            // Store the current quantity before update
            entityStateMap.put(orderItem.getId(), orderItem.getQuantity());
        } catch (Exception e) {
            log.error("Error in order item entity listener preUpdate", e);
        }
    }

    /**
     * Called after an order item is updated
     */
    @PostUpdate
    public void postUpdate(OrderItem orderItem) {
        try {
            // Check if we have previous state
            Integer oldQuantity = entityStateMap.remove(orderItem.getId());

            if (oldQuantity != null && oldQuantity != orderItem.getQuantity()) {
                // Quantity has changed
                if (kafkaService != null) {
                    kafkaService.publishOrderItemUpdated(orderItem.getOrder(), orderItem, oldQuantity);
                    log.debug("JPA listener triggered for order item quantity change: {}", orderItem.getId());
                } else {
                    log.warn("KafkaService is null in OrderItemEntityListener");
                }
            }
        } catch (Exception e) {
            log.error("Error in order item entity listener postUpdate", e);
        }
    }

    /**
     * Called after an order item is created
     */
    @PostPersist
    public void postPersist(OrderItem orderItem) {
        try {
            // New order item was created
            if (kafkaService != null) {
                kafkaService.publishOrderItemAdded(orderItem.getOrder(), orderItem);
                log.debug("JPA listener triggered for order item creation: {}", orderItem.getId());
            } else {
                log.warn("KafkaService is null in OrderItemEntityListener");
            }
        } catch (Exception e) {
            log.error("Error in order item entity listener postPersist", e);
        }
    }
}