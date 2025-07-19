package com.Ecommerce.Loyalty_Service.Payload.Response.CRM;

import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
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
@Schema(description = "CRM user information response")
public class CrmResponseDto {

    @Schema(description = "CRM record ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "Total loyalty points", example = "1250")
    private int totalPoints;

    @Schema(description = "Current membership tier")
    private MembershipTier membershipLevel;

    @Schema(description = "Date when user joined loyalty program")
    private LocalDateTime joinDate;

    @Schema(description = "Last activity timestamp")
    private LocalDateTime lastActivity;

    @Schema(description = "Calculated loyalty score", example = "78.5")
    private Double loyaltyScore;
}

