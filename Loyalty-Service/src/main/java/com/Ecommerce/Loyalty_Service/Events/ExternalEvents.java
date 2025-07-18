package com.Ecommerce.Loyalty_Service.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Events coming from other services that are consumed by the Loyalty Service
 */
public class ExternalEvents {

    /**
     * Event fired when an order is completed
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCompletedEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private UUID orderId;
        private UUID userId;
        private BigDecimal orderTotal;
        private int itemCount;
        private boolean firstOrder;
        private String paymentMethod;
        private String orderStatus;
    }

    /**
     * Event fired when a user is registered
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegisteredEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private UUID userId;
        private String email;
        private LocalDateTime registrationDate;
        private String referredBy;
        private String signupSource;
    }

    /**
     * Event fired when a product review is submitted
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductReviewedEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private UUID userId;
        private UUID productId;
        private int rating;
        private boolean verifiedPurchase;
        private UUID orderId;
    }


    /**
     * Event fired when a cart is abandoned
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartAbandonedEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private UUID cartId;
        private UUID userId;
        private BigDecimal cartTotal;
        private int itemCount;
        private LocalDateTime lastActivityTime;
    }
}