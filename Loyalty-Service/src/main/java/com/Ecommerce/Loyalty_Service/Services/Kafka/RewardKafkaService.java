package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.config.KafkaConfig;
import com.Ecommerce.Loyalty_Service.Entities.LoyaltyReward;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Events.LoyaltyEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for sending Reward events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RewardKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when a reward is redeemed
     */
    public void publishRewardRedeemed(UUID userId, LoyaltyReward reward,
                                      int remainingPoints, MembershipTier membershipTier) {
        LoyaltyEvents.RewardRedeemedEvent event = new LoyaltyEvents.RewardRedeemedEvent(
                reward.getId(),
                reward.getName(),
                userId,
                reward.getPointsCost(),
                remainingPoints,
                membershipTier
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_REWARD_REDEEMED, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published reward redeemed event: {}", event);
                    } else {
                        log.error("Failed to publish reward redeemed event: {}", event, ex);
                    }
                });
    }

    /**
     * Publish an event when a reward is added to the system
     */
    public void publishRewardAdded(LoyaltyReward reward, String addedBy1) {
        // Create an anonymous class or separate event class for reward added events
        Object event = new Object() {
            public final UUID eventId = UUID.randomUUID();
            public final java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
            public final UUID rewardId = reward.getId();
            public final String name = reward.getName();
            public final String description = reward.getDescription();
            public final int pointsCost = reward.getPointsCost();
            public final boolean active = reward.isActive();
            public final int expiryDays = reward.getExpiryDays();
            public final String addedBy = addedBy1;
        };

        kafkaTemplate.send(KafkaConfig.TOPIC_REWARD_ADDED, reward.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published reward added event for reward: {}", reward.getName());
                    } else {
                        log.error("Failed to publish reward added event for reward: {}", reward.getName(), ex);
                    }
                });
    }

    /**
     * Publish an event when a reward is updated
     */
    public void publishRewardUpdated(LoyaltyReward reward, LoyaltyReward previousReward, String updatedBy1) {
        // Create an anonymous class or separate event class for reward updated events
        Object event = new Object() {
            public final UUID eventId = UUID.randomUUID();
            public final java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
            public final UUID rewardId = reward.getId();
            public final String previousName = previousReward.getName();
            public final String newName = reward.getName();
            public final int previousPointsCost = previousReward.getPointsCost();
            public final int newPointsCost = reward.getPointsCost();
            public final boolean previouslyActive = previousReward.isActive();
            public final boolean currentlyActive = reward.isActive();
            public final String updatedBy = updatedBy1;
        };

        kafkaTemplate.send(KafkaConfig.TOPIC_REWARD_UPDATED, reward.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published reward updated event for reward: {}", reward.getName());
                    } else {
                        log.error("Failed to publish reward updated event for reward: {}", reward.getName(), ex);
                    }
                });
    }
}