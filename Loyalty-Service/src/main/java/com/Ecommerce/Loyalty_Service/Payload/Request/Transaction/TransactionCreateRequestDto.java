package com.Ecommerce.Loyalty_Service.Payload.Request.Transaction;

import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new point transaction")
public class TransactionCreateRequestDto {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID for the transaction", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Type of transaction")
    private TransactionType type;

    @Min(value = 1, message = "Points must be at least 1")
    @Schema(description = "Number of points", example = "100")
    private int points;

    @NotBlank(message = "Source is required")
    @Schema(description = "Source of the transaction", example = "Order: 123e4567-e89b-12d3-a456-426614174002")
    private String source;

    @Schema(description = "Related order ID (optional)", example = "123e4567-e89b-12d3-a456-426614174002")
    private UUID relatedOrderId;

    @Schema(description = "Related coupon ID (optional)", example = "123e4567-e89b-12d3-a456-426614174003")
    private UUID relatedCouponId;

    @Schema(description = "Order amount (optional)", example = "150.00")
    private BigDecimal orderAmount;

    @Schema(description = "Idempotency key for duplicate prevention", example = "order-123e4567-e89b-12d3-a456-426614174002")
    private String idempotencyKey;
}