package com.Ecommerce.Loyalty_Service.Payload.Response.Transaction;


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
@Schema(description = "Summary of user's transaction activity")
public class TransactionSummaryDto {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "Total points earned", example = "2500")
    private int totalPointsEarned;

    @Schema(description = "Total points redeemed", example = "750")
    private int totalPointsRedeemed;

    @Schema(description = "Current point balance", example = "1750")
    private int currentBalance;

    @Schema(description = "Total number of transactions", example = "25")
    private int totalTransactions;

    @Schema(description = "Number of earning transactions", example = "20")
    private int earningTransactions;

    @Schema(description = "Number of redemption transactions", example = "5")
    private int redemptionTransactions;

    @Schema(description = "First transaction date")
    private LocalDateTime firstTransactionDate;

    @Schema(description = "Last transaction date")
    private LocalDateTime lastTransactionDate;

    @Schema(description = "Average points per earning transaction", example = "125")
    private double averagePointsPerEarning;
}