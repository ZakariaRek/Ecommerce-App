package com.Ecommerce.Product_Service.Services.Kakfa;


import com.Ecommerce.Product_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Events.InventoryEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishInventoryCreatedEvent(Inventory inventory) {
        try {
            InventoryEvents.InventoryCreatedEvent event = InventoryEvents.InventoryCreatedEvent.builder()
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                    .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                    .quantity(inventory.getQuantity())
                    .lowStockThreshold(inventory.getLowStockThreshold())
                    .warehouseLocation(inventory.getWarehouseLocation())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing inventory created event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_INVENTORY_CREATED, inventory.getId().toString(), event);
            log.info("Inventory created event published successfully for inventory ID: {}", inventory.getId());
        } catch (Exception e) {
            log.error("Failed to publish inventory created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory created event", e);
        }
    }

    public void publishInventoryUpdatedEvent(Inventory inventory) {
        try {
            InventoryEvents.InventoryUpdatedEvent event = InventoryEvents.InventoryUpdatedEvent.builder()
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                    .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                    .quantity(inventory.getQuantity())
                    .lowStockThreshold(inventory.getLowStockThreshold())
                    .warehouseLocation(inventory.getWarehouseLocation())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing inventory updated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_INVENTORY_UPDATED, inventory.getId().toString(), event);
            log.info("Inventory updated event published successfully for inventory ID: {}", inventory.getId());
        } catch (Exception e) {
            log.error("Failed to publish inventory updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory updated event", e);
        }
    }

    public void publishInventoryDeletedEvent(Inventory inventory) {
        try {
            InventoryEvents.InventoryDeletedEvent event = InventoryEvents.InventoryDeletedEvent.builder()
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                    .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing inventory deleted event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_INVENTORY_DELETED, inventory.getId().toString(), event);
            log.info("Inventory deleted event published successfully for inventory ID: {}", inventory.getId());
        } catch (Exception e) {
            log.error("Failed to publish inventory deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory deleted event", e);
        }
    }

    public void publishInventoryStockChangedEvent(Inventory inventory, Integer previousQuantity) {
        try {
            InventoryEvents.InventoryStockChangedEvent event = InventoryEvents.InventoryStockChangedEvent.builder()
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                    .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                    .previousQuantity(previousQuantity)
                    .newQuantity(inventory.getQuantity())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing inventory stock changed event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_INVENTORY_STOCK_CHANGED, inventory.getId().toString(), event);
            log.info("Inventory stock changed event published successfully for inventory ID: {}", inventory.getId());

            // Check if stock is below threshold and send low stock event if needed
            if (inventory.getQuantity() < inventory.getLowStockThreshold()) {
                publishInventoryLowStockEvent(inventory);
            }
        } catch (Exception e) {
            log.error("Failed to publish inventory stock changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory stock changed event", e);
        }
    }

    public void publishInventoryThresholdChangedEvent(Inventory inventory, Integer previousThreshold) {
        try {
            InventoryEvents.InventoryThresholdChangedEvent event = InventoryEvents.InventoryThresholdChangedEvent.builder()
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                    .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                    .previousThreshold(previousThreshold)
                    .newThreshold(inventory.getLowStockThreshold())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing inventory threshold changed event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_INVENTORY_THRESHOLD_CHANGED, inventory.getId().toString(), event);
            log.info("Inventory threshold changed event published successfully for inventory ID: {}", inventory.getId());

            // Check if current quantity is below the new threshold
            if (inventory.getQuantity() < inventory.getLowStockThreshold()) {
                publishInventoryLowStockEvent(inventory);
            }
        } catch (Exception e) {
            log.error("Failed to publish inventory threshold changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory threshold changed event", e);
        }
    }

    public void publishInventoryLowStockEvent(Inventory inventory) {
        try {
            InventoryEvents.InventoryLowStockEvent event = InventoryEvents.InventoryLowStockEvent.builder()
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                    .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                    .currentQuantity(inventory.getQuantity())
                    .lowStockThreshold(inventory.getLowStockThreshold())
                    .warehouseLocation(inventory.getWarehouseLocation())
                    .detectedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing inventory low stock event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_INVENTORY_LOW_STOCK, inventory.getId().toString(), event);
            log.info("Inventory low stock event published successfully for inventory ID: {}", inventory.getId());
        } catch (Exception e) {
            log.error("Failed to publish inventory low stock event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory low stock event", e);
        }
    }

    public void publishInventoryRestockedEvent(Inventory inventory, Integer previousQuantity, Integer addedQuantity) {
        try {
            InventoryEvents.InventoryRestockedEvent event = InventoryEvents.InventoryRestockedEvent.builder()
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                    .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                    .previousQuantity(previousQuantity)
                    .newQuantity(inventory.getQuantity())
                    .addedQuantity(addedQuantity)
                    .warehouseLocation(inventory.getWarehouseLocation())
                    .restockedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing inventory restocked event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_INVENTORY_RESTOCKED, inventory.getId().toString(), event);
            log.info("Inventory restocked event published successfully for inventory ID: {}", inventory.getId());
        } catch (Exception e) {
            log.error("Failed to publish inventory restocked event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory restocked event", e);
        }
    }
}