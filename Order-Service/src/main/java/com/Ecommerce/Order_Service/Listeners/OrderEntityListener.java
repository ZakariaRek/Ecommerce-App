package com.Ecommerce.Order_Service.Listeners;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.KafkaProducers.OrderKafkaService;
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
 * JPA Entity Listener for Order entities to automatically publish events to Kafka
 * when orders are created, updated, or status changes
 */
@Component
@Slf4j
public class OrderEntityListener {

    // Need to use static fields because JPA entity listeners are instantiated by JPA, not Spring
    private static OrderKafkaService kafkaService;

    // Store pre-change state for events
    private static final Map<UUID, OrderStatus> entityStateMap = new ConcurrentHashMap<>();

    @Autowired
    public void setKafkaService(OrderKafkaService kafkaService) {
        OrderEntityListener.kafkaService = kafkaService;
    }

    /**
     * Called before an order is updated
     */
    @PreUpdate
    public void preUpdate(Order order) {
        try {
            // Store the current state before update
            entityStateMap.put(order.getId(), order.getStatus());
        } catch (Exception e) {
            log.error("Error in order entity listener preUpdate", e);
        }
    }

    /**
     * Called after an order is updated
     */
    @PostUpdate
    public void postUpdate(Order order) {
        try {
            // Check if we have previous state
            OrderStatus oldStatus = entityStateMap.remove(order.getId());

            if (oldStatus != null && oldStatus != order.getStatus()) {
                // Status has changed
                if (order.getStatus() == OrderStatus.CANCELED) {
                    // Order was canceled
                    kafkaService.publishOrderCanceled(order, oldStatus);
                    log.debug("JPA listener triggered for order canceled: {}", order.getId());
                } else {
                    // Status changed to something else
                    kafkaService.publishOrderStatusChanged(order, oldStatus);
                    log.debug("JPA listener triggered for order status change: {}", order.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error in order entity listener postUpdate", e);
        }
    }

    /**
     * Called after an order is created
     */
    @PostPersist
    public void postPersist(Order order) {
        try {
            // New order was created
            if (kafkaService != null) {
                kafkaService.publishOrderCreated(order);
                log.debug("JPA listener triggered for order creation: {}", order.getId());
            } else {
                log.warn("KafkaService is null in OrderEntityListener");
            }
        } catch (Exception e) {
            log.error("Error in order entity listener postPersist", e);
        }
    }
}