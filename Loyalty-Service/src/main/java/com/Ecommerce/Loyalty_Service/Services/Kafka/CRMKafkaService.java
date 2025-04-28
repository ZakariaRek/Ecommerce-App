package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.Config.KafkaConfig;
import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Events.LoyaltyEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for sending CRM events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CRMKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when points are earned
     */
    public void publishPointsEarned(UUID userId, int pointsEarned, int totalPoints,
                                    String source, UUID sourceId, MembershipTier membershipTier) {
        LoyaltyEvents.PointsEarnedEvent event = new LoyaltyEvents.PointsEarnedEvent(
                userId, pointsEarned, totalPoints, source, sourceId, membershipTier
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_POINTS_EARNED, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published points earned event: {}", event);
                    } else {
                        log.error("Failed to publish points earned event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when points are redeemed
     */
    public void publishPointsRedeemed(UUID userId, int pointsRedeemed, int remainingPoints,
                                      String purpose, UUID purposeId, MembershipTier membershipTier) {
        LoyaltyEvents.PointsRedeemedEvent event = new LoyaltyEvents.PointsRedeemedEvent(
                userId, pointsRedeemed, remainingPoints, purpose, purposeId, membershipTier
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_POINTS_REDEEMED, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published points redeemed event: {}", event);
                    } else {
                        log.error("Failed to publish points redeemed event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when membership tier changes
     */
    public void publishMembershipTierChanged(UUID userId, MembershipTier previousTier,
                                             MembershipTier newTier, int totalPoints, String reason) {
        LoyaltyEvents.MembershipTierChangedEvent event = new LoyaltyEvents.MembershipTierChangedEvent(
                userId, previousTier, newTier, totalPoints, reason
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_MEMBERSHIP_CHANGED, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published membership tier changed event: {}", event);
                    } else {
                        log.error("Failed to publish membership tier changed event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when points are adjusted
     */
    public void publishPointsAdjusted(UUID userId, int pointsAdjusted, int totalPoints,
                                      String reason, String adjustedBy, MembershipTier membershipTier) {
        LoyaltyEvents.PointsAdjustedEvent event = new LoyaltyEvents.PointsAdjustedEvent(
                userId, pointsAdjusted, totalPoints, reason, adjustedBy, membershipTier
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_POINTS_ADJUSTED, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published points adjusted event: {}", event);
                    } else {
                        log.error("Failed to publish points adjusted event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when points expire
     */
    public void publishPointsExpired(CRM crm, int pointsExpired) {
        LoyaltyEvents.PointsExpiredEvent event = new LoyaltyEvents.PointsExpiredEvent(
                crm.getUserId(),
                pointsExpired,
                crm.getTotalPoints(),
                crm.getLastActivity().plusDays(365), // Assuming points expire after 1 year
                crm.getMembershipLevel()
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_POINTS_EXPIRED, crm.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published points expired event: {}", event);
                    } else {
                        log.error("Failed to publish points expired event: {}", event, ex);
                    }
                });
    }
}