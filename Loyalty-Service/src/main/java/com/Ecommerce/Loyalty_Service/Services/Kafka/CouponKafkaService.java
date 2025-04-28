package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.Config.KafkaConfig;
import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Events.LoyaltyEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for sending Coupon events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CouponKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when a coupon is generated
     */
    public void publishCouponGenerated(Coupon coupon, String generationReason) {
        LoyaltyEvents.CouponGeneratedEvent event = new LoyaltyEvents.CouponGeneratedEvent(
                coupon.getId(),
                coupon.getCode(),
                coupon.getUserId(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinPurchaseAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getExpirationDate(),
                generationReason
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_COUPON_GENERATED, coupon.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published coupon generated event: {}", event);
                    } else {
                        log.error("Failed to publish coupon generated event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when a coupon is redeemed
     */
    public void publishCouponRedeemed(Coupon coupon, BigDecimal originalAmount,
                                      BigDecimal discountAmount, BigDecimal finalAmount, UUID orderId) {
        LoyaltyEvents.CouponRedeemedEvent event = new LoyaltyEvents.CouponRedeemedEvent(
                coupon.getId(),
                coupon.getCode(),
                coupon.getUserId(),
                originalAmount,
                discountAmount,
                finalAmount,
                orderId
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_COUPON_REDEEMED, coupon.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published coupon redeemed event: {}", event);
                    } else {
                        log.error("Failed to publish coupon redeemed event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when a coupon expires
     */
    public void publishCouponExpired(Coupon coupon) {
        LoyaltyEvents.CouponExpiredEvent event = new LoyaltyEvents.CouponExpiredEvent(
                coupon.getId(),
                coupon.getCode(),
                coupon.getUserId(),
                coupon.getExpirationDate()
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_COUPON_EXPIRED, coupon.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published coupon expired event: {}", event);
                    } else {
                        log.error("Failed to publish coupon expired event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when a coupon is validated
     */
    public void publishCouponValidated(String couponCode, UUID userId1, boolean isValid,
                                       BigDecimal purchaseAmount1, String validationMessage) {
        // Create an anonymous class or separate event class for validation events
        Object event = new Object() {
            public final UUID eventId = UUID.randomUUID();
            public final LocalDateTime timestamp = java.time.LocalDateTime.now();
            public final String code = couponCode;
            public final UUID userId = userId1;
            public final boolean valid = isValid;
            public final BigDecimal purchaseAmount = purchaseAmount1;
            public final String message = validationMessage;
        };

        kafkaTemplate.send(KafkaConfig.TOPIC_COUPON_VALIDATED, userId1.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published coupon validated event for code: {}", couponCode);
                    } else {
                        log.error("Failed to publish coupon validated event for code: {}", couponCode, ex);
                    }
                });
    }
}