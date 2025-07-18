package com.Ecommerce.Product_Service.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReviewEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewCreatedEvent {
        private UUID reviewId;
        private UUID userId;
        private UUID productId;
        private String productName;
        private Integer rating;
        private String comment;
        private Boolean verified;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewUpdatedEvent {
        private UUID reviewId;
        private UUID userId;
        private UUID productId;
        private String productName;
        private Integer rating;
        private String comment;
        private Boolean verified;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewDeletedEvent {
        private UUID reviewId;
        private UUID userId;
        private UUID productId;
        private String productName;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewVerifiedEvent {
        private UUID reviewId;
        private UUID userId;
        private UUID productId;
        private String productName;
        private Integer rating;
        private LocalDateTime verifiedAt;
    }


}