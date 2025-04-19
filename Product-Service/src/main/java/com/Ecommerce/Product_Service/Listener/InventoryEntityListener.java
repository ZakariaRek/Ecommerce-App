package com.Ecommerce.Product_Service.Listener;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Services.Kakfa.InventoryEventService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InventoryEntityListener {

    private static InventoryEventService inventoryEventService;

    @Autowired
    public void setInventoryEventService(InventoryEventService inventoryEventService) {
        InventoryEntityListener.inventoryEventService = inventoryEventService;
    }

    @PostPersist
    public void postPersist(Inventory inventory) {
        if (inventoryEventService != null) {
            inventoryEventService.publishInventoryCreatedEvent(inventory);
        }
    }

    @PostUpdate
    public void postUpdate(Inventory inventory) {
        if (inventoryEventService != null) {
            // Basic update event
            inventoryEventService.publishInventoryUpdatedEvent(inventory);

            // The @PostUpdate doesn't provide the previous state
            // In a real application, you would need to use @PreUpdate to capture previous values
            // and store them in thread-local variables or a similar mechanism
        }
    }

    @PostRemove
    public void postRemove(Inventory inventory) {
        if (inventoryEventService != null) {
            inventoryEventService.publishInventoryDeletedEvent(inventory);
        }
    }

    @PreUpdate
    public void preUpdate(Inventory inventory) {
        // In a real application, you would store the current state before the update
        // For example, using a ThreadLocal variable or similar mechanism
        // ThreadLocalContext.setPreviousQuantity(inventory.getQuantity());
        // ThreadLocalContext.setPreviousThreshold(inventory.getLowStockThreshold());
    }
}