package com.Ecommerce.Product_Service.Events;
import com.Ecommerce.Product_Service.Entities.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class DiscountEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountCreatedEvent {
        private UUID discountId;
        private UUID productId;
        private String productName;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal minPurchaseAmount;
        private BigDecimal maxDiscountAmount;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountUpdatedEvent {
        private UUID discountId;
        private UUID productId;
        private String productName;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal minPurchaseAmount;
        private BigDecimal maxDiscountAmount;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountDeletedEvent {
        private UUID discountId;
        private UUID productId;
        private String productName;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountActivatedEvent {
        private UUID discountId;
        private UUID productId;
        private String productName;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal originalPrice;
        private BigDecimal discountedPrice;
        private LocalDateTime activatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountDeactivatedEvent {
        private UUID discountId;
        private UUID productId;
        private String productName;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private LocalDateTime deactivatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountValueChangedEvent {
        private UUID discountId;
        private UUID productId;
        private String productName;
        private DiscountType discountType;
        private BigDecimal previousDiscountValue;
        private BigDecimal newDiscountValue;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountPeriodChangedEvent {
        private UUID discountId;
        private UUID productId;
        private String productName;
        private DiscountType discountType;
        private LocalDateTime previousStartDate;
        private LocalDateTime previousEndDate;
        private LocalDateTime newStartDate;
        private LocalDateTime newEndDate;
        private LocalDateTime updatedAt;
    }
}