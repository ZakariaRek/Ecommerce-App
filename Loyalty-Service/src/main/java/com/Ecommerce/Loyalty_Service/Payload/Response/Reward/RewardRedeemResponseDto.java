package com.Ecommerce.Loyalty_Service.Payload.Response.Reward;



import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reward redemption result")
public class RewardRedeemResponseDto {

    @Schema(description = "Redemption transaction ID", example = "123e4567-e89b-12d3-a456-426614174002")
    private UUID transactionId;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "Redeemed reward information")
    private RewardResponseDto reward;

    @Schema(description = "Points cost for this redemption", example = "500")
    private int pointsDeducted;

    @Schema(description = "User's remaining points after redemption", example = "250")
    private int remainingPoints;

    @Schema(description = "Redemption timestamp")
    private LocalDateTime redeemedAt;

    @Schema(description = "Success message", example = "Reward redeemed successfully!")
    private String message;

    @Schema(description = "Instructions for reward usage", example = "Your gift card code will be sent to your email within 24 hours")
    private String instructions;
}