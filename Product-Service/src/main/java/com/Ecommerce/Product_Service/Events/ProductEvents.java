package com.Ecommerce.Product_Service.Events;

import com.Ecommerce.Product_Service.Entities.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ProductEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCreatedEvent {
        private UUID productId;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private String sku;
        private BigDecimal weight;
        private String dimensions;
        private List<String> images;
        private ProductStatus status;
        private List<UUID> categoryIds;
        private List<UUID> supplierIds;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductUpdatedEvent {
        private UUID productId;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private String sku;
        private BigDecimal weight;
        private String dimensions;
        private List<String> images;
        private ProductStatus status;
        private List<UUID> categoryIds;
        private List<UUID> supplierIds;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDeletedEvent {
        private UUID productId;
        private String name;
        private String sku;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStockChangedEvent {
        private UUID productId;
        private String name;
        private String sku;
        private Integer previousStock;
        private Integer newStock;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPriceChangedEvent {
        private UUID productId;
        private String name;
        private String sku;
        private BigDecimal previousPrice;
        private BigDecimal newPrice;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStatusChangedEvent {
        private UUID productId;
        private String name;
        private String sku;
        private ProductStatus previousStatus;
        private ProductStatus newStatus;
        private LocalDateTime updatedAt;
    }
}