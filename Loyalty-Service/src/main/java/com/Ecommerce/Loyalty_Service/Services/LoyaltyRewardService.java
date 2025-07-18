package com.Ecommerce.Loyalty_Service.Services;


import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.LoyaltyReward;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Repositories.LoyaltyRewardRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.RewardKafkaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyRewardService {
    private final LoyaltyRewardRepository rewardRepository;
    private final PointTransactionService transactionService;
    private final CRMService crmService;
    private final RewardKafkaService kafkaService;

    public List<LoyaltyReward> getActiveRewards() {
        return rewardRepository.findByIsActiveTrue();
    }

    @Transactional
    public void redeemReward(UUID userId, UUID rewardId) {
        LoyaltyReward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Reward not found"));

        if (!reward.isActive()) {
            throw new RuntimeException("Reward is no longer active");
        }

        // Get user CRM record to check points and membership tier
        CRM userCRM = crmService.getByUserId(userId);

        // Check if user has enough points
        if (userCRM.getTotalPoints() < reward.getPointsCost()) {
            throw new RuntimeException("Insufficient points to redeem this reward");
        }

        // Record transaction and deduct points
        transactionService.recordTransaction(
                userId,
                TransactionType.REDEEM,
                reward.getPointsCost(),
                "Reward: " + reward.getName()
        );

        // Get updated user info after points deduction
        CRM updatedCRM = crmService.getByUserId(userId);

        // Direct Kafka event
        kafkaService.publishRewardRedeemed(
                userId,
                reward,
                updatedCRM.getTotalPoints(),
                updatedCRM.getMembershipLevel()
        );

        log.info("User {} redeemed reward {}: {} for {} points",
                userId, reward.getId(), reward.getName(), reward.getPointsCost());

        // Here you could trigger other actions like generating a coupon,
        // sending an email with a discount code, etc.
    }

    /**
     * Add a new reward
     */
    @Transactional
    public LoyaltyReward addReward(String name, String description, int pointsCost, boolean isActive, int expiryDays) {
        LoyaltyReward reward = new LoyaltyReward();
        reward.setId(UUID.randomUUID());
        reward.setName(name);
        reward.setDescription(description);
        reward.setPointsCost(pointsCost);
        reward.setActive(isActive);
        reward.setExpiryDays(expiryDays);

        // Save the reward (MongoDB listener will trigger event)
        LoyaltyReward savedReward = rewardRepository.save(reward);

        // Direct Kafka event
        kafkaService.publishRewardAdded(savedReward, "ADMIN");

        log.info("Added new reward: {}", savedReward.getName());

        return savedReward;
    }

    /**
     * Update an existing reward
     */
    @Transactional
    public LoyaltyReward updateReward(UUID rewardId, String name, String description,
                                      int pointsCost, boolean isActive, int expiryDays) {
        LoyaltyReward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Reward not found"));



        // Update fields
        reward.setName(name);
        reward.setDescription(description);
        reward.setPointsCost(pointsCost);
        reward.setActive(isActive);
        reward.setExpiryDays(expiryDays);

        // Save changes (MongoDB listener will trigger event)
        LoyaltyReward updatedReward = rewardRepository.save(reward);

        log.info("Updated reward: {}", updatedReward.getName());

        return updatedReward;
    }

    /**
     * Deactivate a reward
     */
    @Transactional
    public void deactivateReward(UUID rewardId) {
        LoyaltyReward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new RuntimeException("Reward not found"));

        if (!reward.isActive()) {
            return; // Already inactive
        }


        // Deactivate
        reward.setActive(false);

        // Save changes (MongoDB listener will trigger event)
        rewardRepository.save(reward);

        log.info("Deactivated reward: {}", reward.getName());
    }
}