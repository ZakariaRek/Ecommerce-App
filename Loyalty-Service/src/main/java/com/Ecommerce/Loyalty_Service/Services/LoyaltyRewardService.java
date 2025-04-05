package com.Ecommerce.Loyalty_Service.Services;


import com.Ecommerce.Loyalty_Service.Entities.LoyaltyReward;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Repositories.LoyaltyRewardRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LoyaltyRewardService {
    @Autowired
    private LoyaltyRewardRepository rewardRepository;

    @Autowired
    private PointTransactionService transactionService;

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

        // Record transaction and deduct points
        transactionService.recordTransaction(
                userId,
                TransactionType.REDEEM,
                reward.getPointsCost(),
                "Reward: " + reward.getName()
        );

        // Here you could trigger other actions like generating a coupon,
        // sending an email with a discount code, etc.
    }
}