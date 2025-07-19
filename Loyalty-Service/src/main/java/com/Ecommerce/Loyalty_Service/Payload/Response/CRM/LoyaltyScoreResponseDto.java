package com.Ecommerce.Loyalty_Service.Payload.Response.CRM;
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
@Schema(description = "Loyalty score calculation response")
public class LoyaltyScoreResponseDto {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "Calculated loyalty score", example = "78.5")
    private double loyaltyScore;

    @Schema(description = "Score breakdown explanation", example = "Based on 1250 points, 180 days membership, recent activity")
    private String explanation;
}