package com.Ecommerce.Loyalty_Service.Payload.Request.Reward;


import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to redeem a reward")
public class RewardRedeemRequestDto {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID who is redeeming the reward", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @NotNull(message = "Reward ID is required")
    @Schema(description = "Reward ID to redeem", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID rewardId;
}