package com.Ecommerce.Loyalty_Service.Controllers;


import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.LoyaltyReward;
import com.Ecommerce.Loyalty_Service.Mappers.RewardMapper;
import com.Ecommerce.Loyalty_Service.Payload.Request.Reward.RewardRedeemRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Reward.RewardRedeemResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Reward.RewardResponseDto;
import com.Ecommerce.Loyalty_Service.Services.CRMService;
import com.Ecommerce.Loyalty_Service.Services.LoyaltyRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rewards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reward Management", description = "Loyalty reward operations")
public class RewardController {

    private final LoyaltyRewardService rewardService;
    private final CRMService crmService;
    private final RewardMapper rewardMapper;

    @Operation(
            summary = "Get all active rewards",
            description = "Retrieve all currently active loyalty rewards available for redemption"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved active rewards"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<RewardResponseDto>> getActiveRewards(
            @Parameter(description = "Optional user ID to check affordability",
                    example = "123e4567-e89b-12d3-a456-426614174001")
            @RequestParam(required = false) UUID userId) {
        log.info("Retrieving active rewards, userId: {}", userId);
        List<LoyaltyReward> rewards = rewardService.getActiveRewards();

        List<RewardResponseDto> rewardDtos;
        if (userId != null) {
            try {
                CRM userCrm = crmService.getByUserId(userId);
                int userPoints = userCrm.getTotalPoints();
                rewardDtos = rewards.stream()
                        .map(reward -> rewardMapper.toResponseDto(reward, userPoints))
                        .collect(Collectors.toList());
            } catch (RuntimeException e) {
                // User not found in CRM, return rewards without affordability check
                log.warn("User {} not found in CRM, returning rewards without affordability check", userId);
                rewardDtos = rewards.stream()
                        .map(rewardMapper::toResponseDto)
                        .collect(Collectors.toList());
            }
        } else {
            rewardDtos = rewards.stream()
                    .map(rewardMapper::toResponseDto)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(rewardDtos);
    }

    @Operation(
            summary = "Redeem a reward",
            description = "Redeem a loyalty reward using points"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reward redeemed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient points"),
            @ApiResponse(responseCode = "404", description = "Reward or user not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{rewardId}/redeem")
    public ResponseEntity<RewardRedeemResponseDto> redeemReward(
            @Parameter(description = "Reward ID to redeem", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID rewardId,
            @Valid @RequestBody RewardRedeemRequestDto request) {
        log.info("Redeeming reward: {} for user: {}", rewardId, request.getUserId());

        // Get reward details before redemption
        List<LoyaltyReward> rewards = rewardService.getActiveRewards();
        LoyaltyReward reward = rewards.stream()
                .filter(r -> r.getId().equals(rewardId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Reward not found"));

        // Get user points before redemption
        CRM userCrm = crmService.getByUserId(request.getUserId());

        // Perform redemption
        rewardService.redeemReward(request.getUserId(), rewardId);

        // Get updated user points
        CRM updatedCrm = crmService.getByUserId(request.getUserId());

        // Generate instructions based on reward type
        String instructions = generateRedemptionInstructions(reward);

        RewardRedeemResponseDto responseDto = RewardRedeemResponseDto.builder()
                .transactionId(UUID.randomUUID()) // In a real scenario, you'd get this from the transaction
                .userId(request.getUserId())
                .reward(rewardMapper.toResponseDto(reward))
                .pointsDeducted(reward.getPointsCost())
                .remainingPoints(updatedCrm.getTotalPoints())
                .redeemedAt(LocalDateTime.now())
                .message("Reward redeemed successfully!")
                .instructions(instructions)
                .build();

        return ResponseEntity.ok(responseDto);
    }

    private String generateRedemptionInstructions(LoyaltyReward reward) {
        if (reward.getName().toLowerCase().contains("gift card")) {
            return "Your gift card code will be sent to your email within 24 hours.";
        } else if (reward.getName().toLowerCase().contains("shipping")) {
            return "Free shipping will be automatically applied to your next order.";
        } else if (reward.getName().toLowerCase().contains("coupon")) {
            return "Your discount coupon has been added to your account and can be used at checkout.";
        } else {
            return "Your reward has been processed. Check your account for details.";
        }
    }
}