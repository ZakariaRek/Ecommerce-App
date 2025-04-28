package com.Ecommerce.Product_Service.Events;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryCreatedEvent {
        private UUID inventoryId;
        private UUID productId;
        private String productName;
        private Integer quantity;
        private Integer lowStockThreshold;
        private String warehouseLocation;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryUpdatedEvent {
        private UUID inventoryId;
        private UUID productId;
        private String productName;
        private Integer quantity;
        private Integer lowStockThreshold;
        private String warehouseLocation;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryDeletedEvent {
        private UUID inventoryId;
        private UUID productId;
        private String productName;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryStockChangedEvent {
        private UUID inventoryId;
        private UUID productId;
        private String productName;
        private Integer previousQuantity;
        private Integer newQuantity;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryThresholdChangedEvent {
        private UUID inventoryId;
        private UUID productId;
        private String productName;
        private Integer previousThreshold;
        private Integer newThreshold;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryLowStockEvent {
        private UUID inventoryId;
        private UUID productId;
        private String productName;
        private Integer currentQuantity;
        private Integer lowStockThreshold;
        private String warehouseLocation;
        private LocalDateTime detectedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryRestockedEvent {
        private UUID inventoryId;
        private UUID productId;
        private String productName;
        private Integer previousQuantity;
        private Integer newQuantity;
        private Integer addedQuantity;
        private String warehouseLocation;
        private LocalDateTime restockedAt;
    }
}