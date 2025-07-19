package com.Ecommerce.Loyalty_Service.Payload.Response.Reward;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Loyalty reward information")
public class RewardResponseDto {

    @Schema(description = "Reward ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Reward name", example = "$10 Gift Card")
    private String name;

    @Schema(description = "Reward description", example = "Redeem for a $10 gift card to use on any purchase")
    private String description;

    @Schema(description = "Points required to redeem", example = "500")
    private int pointsCost;

    @Schema(description = "Whether reward is currently active", example = "true")
    private boolean isActive;

    @Schema(description = "Expiry days after redemption", example = "90")
    private int expiryDays;

    @Schema(description = "Whether user can afford this reward", example = "true")
    private Boolean canAfford;

    @Schema(description = "User's current points (for affordability check)", example = "750")
    private Integer userCurrentPoints;
}