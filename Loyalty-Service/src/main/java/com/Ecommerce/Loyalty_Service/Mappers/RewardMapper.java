package com.Ecommerce.Loyalty_Service.Mappers;


import com.Ecommerce.Loyalty_Service.Entities.LoyaltyReward;
import com.Ecommerce.Loyalty_Service.Payload.Response.Reward.RewardResponseDto;
import org.springframework.stereotype.Component;

@Component
public class RewardMapper {

    public RewardResponseDto toResponseDto(LoyaltyReward reward) {
        return RewardResponseDto.builder()
                .id(reward.getId())
                .name(reward.getName())
                .description(reward.getDescription())
                .pointsCost(reward.getPointsCost())
                .isActive(reward.isActive())
                .expiryDays(reward.getExpiryDays())
                .build();
    }

    public RewardResponseDto toResponseDto(LoyaltyReward reward, int userPoints) {
        return RewardResponseDto.builder()
                .id(reward.getId())
                .name(reward.getName())
                .description(reward.getDescription())
                .pointsCost(reward.getPointsCost())
                .isActive(reward.isActive())
                .expiryDays(reward.getExpiryDays())
                .canAfford(userPoints >= reward.getPointsCost())
                .userCurrentPoints(userPoints)
                .build();
    }
}