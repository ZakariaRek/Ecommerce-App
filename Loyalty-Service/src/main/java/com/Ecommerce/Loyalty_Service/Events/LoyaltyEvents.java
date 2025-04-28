package com.Ecommerce.Loyalty_Service.Events;

import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoyaltyEvents {

    /**
     * Base event for all loyalty events
     */
    @Data
    @NoArgsConstructor
     public abstract static class BaseLoyaltyEvent {
        private UUID eventId;
        private LocalDateTime timestamp;
        private String eventType;
        private UUID userId;
        private UUID sessionId;

        public BaseLoyaltyEvent(String eventType) {
            this.eventId = UUID.randomUUID();
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
        }
    }

    /**
     * Event fired when loyalty points are earned
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class PointsEarnedEvent extends BaseLoyaltyEvent {
        private UUID userId;
        private int pointsEarned;
        private int totalPoints;
        private String source; // ORDER, REVIEW, SIGNUP, etc.
        private UUID sourceId; // ID of the source (order ID, review ID, etc.)
        private MembershipTier membershipTier;

        public PointsEarnedEvent(UUID userId, int pointsEarned, int totalPoints,
                                 String source, UUID sourceId, MembershipTier membershipTier) {
            super("POINTS_EARNED");
            this.userId = userId;
            this.pointsEarned = pointsEarned;
            this.totalPoints = totalPoints;
            this.source = source;
            this.sourceId = sourceId;
            this.membershipTier = membershipTier;
        }
    }

    /**
     * Event fired when loyalty points are redeemed
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class PointsRedeemedEvent extends BaseLoyaltyEvent {
        private UUID userId;
        private int pointsRedeemed;
        private int remainingPoints;
        private String purpose; // COUPON, REWARD, etc.
        private UUID purposeId; // ID of the coupon, reward, etc.
        private MembershipTier membershipTier;

        public PointsRedeemedEvent(UUID userId, int pointsRedeemed, int remainingPoints,
                                   String purpose, UUID purposeId, MembershipTier membershipTier) {
            super("POINTS_REDEEMED");
            this.userId = userId;
            this.pointsRedeemed = pointsRedeemed;
            this.remainingPoints = remainingPoints;
            this.purpose = purpose;
            this.purposeId = purposeId;
            this.membershipTier = membershipTier;
        }
    }

    /**
     * Event fired when loyalty points expire
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class PointsExpiredEvent extends BaseLoyaltyEvent {
        private UUID userId;
        private int pointsExpired;
        private int remainingPoints;
        private LocalDateTime expirationDate;
        private MembershipTier membershipTier;

        public PointsExpiredEvent(UUID userId, int pointsExpired, int remainingPoints,
                                  LocalDateTime expirationDate, MembershipTier membershipTier) {
            super("POINTS_EXPIRED");
            this.userId = userId;
            this.pointsExpired = pointsExpired;
            this.remainingPoints = remainingPoints;
            this.expirationDate = expirationDate;
            this.membershipTier = membershipTier;
        }
    }

    /**
     * Event fired when loyalty points are adjusted (manual admin adjustment)
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class PointsAdjustedEvent extends BaseLoyaltyEvent {
        private UUID userId;
        private int pointsAdjusted; // can be positive or negative
        private int totalPoints;
        private String reason;
        private String adjustedBy; // admin ID or system ID
        private MembershipTier membershipTier;

        public PointsAdjustedEvent(UUID userId, int pointsAdjusted, int totalPoints,
                                   String reason, String adjustedBy, MembershipTier membershipTier) {
            super("POINTS_ADJUSTED");
            this.userId = userId;
            this.pointsAdjusted = pointsAdjusted;
            this.totalPoints = totalPoints;
            this.reason = reason;
            this.adjustedBy = adjustedBy;
            this.membershipTier = membershipTier;
        }
    }

    /**
     * Event fired when a user's membership tier changes
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class MembershipTierChangedEvent extends BaseLoyaltyEvent {
        private UUID userId;
        private MembershipTier previousTier;
        private MembershipTier newTier;
        private int totalPoints;
        private String reason; // POINTS_INCREASE, POINTS_DECREASE, ADMIN_ADJUSTMENT, etc.

        public MembershipTierChangedEvent(UUID userId, MembershipTier previousTier, MembershipTier newTier,
                                          int totalPoints, String reason) {
            super("MEMBERSHIP_TIER_CHANGED");
            this.userId = userId;
            this.previousTier = previousTier;
            this.newTier = newTier;
            this.totalPoints = totalPoints;
            this.reason = reason;
        }
    }

    /**
     * Event fired when a membership tier benefits are updated
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class MembershipBenefitsUpdatedEvent extends BaseLoyaltyEvent {
        private MembershipTier tier;
        private String previousBenefits;
        private String newBenefits;
        private String updatedBy;

        public MembershipBenefitsUpdatedEvent(MembershipTier tier, String previousBenefits,
                                              String newBenefits, String updatedBy) {
            super("MEMBERSHIP_BENEFITS_UPDATED");
            this.tier = tier;
            this.previousBenefits = previousBenefits;
            this.newBenefits = newBenefits;
            this.updatedBy = updatedBy;
        }
    }

    /**
     * Event fired when a coupon is generated
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class CouponGeneratedEvent extends BaseLoyaltyEvent {
        private UUID couponId;
        private String couponCode;
        private UUID userId;
        private DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal minPurchaseAmount;
        private BigDecimal maxDiscountAmount;
        private LocalDateTime expirationDate;
        private String generationReason; // POINTS_REDEMPTION, BIRTHDAY, WELCOME, etc.

        public CouponGeneratedEvent(UUID couponId, String couponCode, UUID userId, DiscountType discountType,
                                    BigDecimal discountValue, BigDecimal minPurchaseAmount,
                                    BigDecimal maxDiscountAmount, LocalDateTime expirationDate,
                                    String generationReason) {
            super("COUPON_GENERATED");
            this.couponId = couponId;
            this.couponCode = couponCode;
            this.userId = userId;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.minPurchaseAmount = minPurchaseAmount;
            this.maxDiscountAmount = maxDiscountAmount;
            this.expirationDate = expirationDate;
            this.generationReason = generationReason;
        }
    }

    /**
     * Event fired when a coupon is redeemed
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class CouponRedeemedEvent extends BaseLoyaltyEvent {
        private UUID couponId;
        private String couponCode;
        private UUID userId;
        private BigDecimal originalAmount;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private UUID orderId;

        public CouponRedeemedEvent(UUID couponId, String couponCode, UUID userId, BigDecimal originalAmount,
                                   BigDecimal discountAmount, BigDecimal finalAmount, UUID orderId) {
            super("COUPON_REDEEMED");
            this.couponId = couponId;
            this.couponCode = couponCode;
            this.userId = userId;
            this.originalAmount = originalAmount;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
            this.orderId = orderId;
        }
    }

    /**
     * Event fired when a coupon expires
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class CouponExpiredEvent extends BaseLoyaltyEvent {
        private UUID couponId;
        private String couponCode;
        private UUID userId;
        private LocalDateTime expirationDate;

        public CouponExpiredEvent(UUID couponId, String couponCode, UUID userId,
                                  LocalDateTime expirationDate) {
            super("COUPON_EXPIRED");
            this.couponId = couponId;
            this.couponCode = couponCode;
            this.userId = userId;
            this.expirationDate = expirationDate;
        }
    }

    /**
     * Event fired when a reward is redeemed
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class RewardRedeemedEvent extends BaseLoyaltyEvent {
        private UUID rewardId;
        private String rewardName;
        private UUID userId;
        private int pointsCost;
        private int remainingPoints;
        private MembershipTier membershipTier;

        public RewardRedeemedEvent(UUID rewardId, String rewardName, UUID userId, int pointsCost,
                                   int remainingPoints, MembershipTier membershipTier) {
            super("REWARD_REDEEMED");
            this.rewardId = rewardId;
            this.rewardName = rewardName;
            this.userId = userId;
            this.pointsCost = pointsCost;
            this.remainingPoints = remainingPoints;
            this.membershipTier = membershipTier;
        }
    }

    /**
     * Event fired when a transaction is recorded
     */
    @Data
    @Builder
    @NoArgsConstructor
     public static class TransactionRecordedEvent extends BaseLoyaltyEvent {
        private UUID transactionId;
        private UUID userId;
        private TransactionType type;
        private int points;
        private String source;
        private int balance;

        public TransactionRecordedEvent(UUID transactionId, UUID userId, TransactionType type,
                                        int points, String source, int balance) {
            super("TRANSACTION_RECORDED");
            this.transactionId = transactionId;
            this.userId = userId;
            this.type = type;
            this.points = points;
            this.source = source;
            this.balance = balance;
        }
    }
}