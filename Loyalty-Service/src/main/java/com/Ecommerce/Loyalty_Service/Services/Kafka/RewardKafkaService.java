package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.Config.KafkaConfig;
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



}