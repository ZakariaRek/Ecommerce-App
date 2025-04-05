package com.Ecommerce.Loyalty_Service.Controllers;

import java.util.List;
import java.util.UUID;

import com.Ecommerce.Loyalty_Service.Entities.LoyaltyReward;
import com.Ecommerce.Loyalty_Service.Services.LoyaltyRewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/rewards")
public class RewardController {
    @Autowired
    private LoyaltyRewardService rewardService;

    @GetMapping
    public ResponseEntity<List<LoyaltyReward>> getActiveRewards() {
        return ResponseEntity.ok(rewardService.getActiveRewards());
    }

    @PostMapping("/{rewardId}/redeem")
    public ResponseEntity<Void> redeemReward(
            @PathVariable UUID rewardId,
            @RequestParam UUID userId) {
        rewardService.redeemReward(userId, rewardId);
        return ResponseEntity.ok().build();
    }
}