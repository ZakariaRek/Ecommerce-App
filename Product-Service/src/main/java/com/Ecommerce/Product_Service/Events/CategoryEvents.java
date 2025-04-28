package com.Ecommerce.Product_Service.Events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CategoryEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryCreatedEvent {
        private UUID categoryId;
        private String name;
        private UUID parentId;
        private String description;
        private String imageUrl;
        private Integer level;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryUpdatedEvent {
        private UUID categoryId;
        private String name;
        private UUID parentId;
        private String description;
        private String imageUrl;
        private Integer level;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDeletedEvent {
        private UUID categoryId;
        private String name;
        private UUID parentId;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryHierarchyChangedEvent {
        private UUID categoryId;
        private String categoryName;
        private UUID previousParentId;
        private UUID newParentId;
        private String previousFullPath;
        private String newFullPath;
        private Integer previousLevel;
        private Integer newLevel;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryProductAssociationEvent {
        private UUID categoryId;
        private String categoryName;
        private UUID productId;
        private String productName;
        private boolean associated; // true if product added to category, false if removed
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryImageUpdatedEvent {
        private UUID categoryId;
        private String categoryName;
        private String previousImageUrl;
        private String newImageUrl;
        private LocalDateTime updatedAt;
    }
}