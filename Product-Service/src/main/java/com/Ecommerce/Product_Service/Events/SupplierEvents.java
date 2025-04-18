package com.Ecommerce.Product_Service.Events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SupplierEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierCreatedEvent {
        private UUID supplierId;
        private String name;
        private String contactInfo;
        private String address;
        private Map<String, Object> contractDetails;
        private BigDecimal rating;
        private List<UUID> productIds;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierUpdatedEvent {
        private UUID supplierId;
        private String name;
        private String contactInfo;
        private String address;
        private Map<String, Object> contractDetails;
        private BigDecimal rating;
        private List<UUID> productIds;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierDeletedEvent {
        private UUID supplierId;
        private String name;
        private String contactInfo;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierRatingChangedEvent {
        private UUID supplierId;
        private String name;
        private BigDecimal previousRating;
        private BigDecimal newRating;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierContractUpdatedEvent {
        private UUID supplierId;
        private String name;
        private Map<String, Object> previousContractDetails;
        private Map<String, Object> newContractDetails;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierProductAssociationEvent {
        private UUID supplierId;
        private String supplierName;
        private UUID productId;
        private String productName;
        private boolean associated; // true if associated, false if dissociated
        private LocalDateTime timestamp;
    }
}