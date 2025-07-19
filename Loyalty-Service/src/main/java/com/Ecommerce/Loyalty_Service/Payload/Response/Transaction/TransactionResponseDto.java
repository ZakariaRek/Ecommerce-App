package com.Ecommerce.Loyalty_Service.Payload.Response.Transaction;

import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Point transaction information")
public class TransactionResponseDto {

    @Schema(description = "Transaction ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "Transaction type")
    private TransactionType type;

    @Schema(description = "Points amount", example = "100")
    private int points;

    @Schema(description = "Transaction date")
    private LocalDateTime transactionDate;

    @Schema(description = "Transaction source", example = "Order: 123e4567-e89b-12d3-a456-426614174002")
    private String source;

    @Schema(description = "Balance after transaction", example = "1250")
    private int balance;

    @Schema(description = "Related order ID", example = "123e4567-e89b-12d3-a456-426614174002")
    private UUID relatedOrderId;

    @Schema(description = "Related coupon ID", example = "123e4567-e89b-12d3-a456-426614174003")
    private UUID relatedCouponId;

    @Schema(description = "Points expiration date")
    private LocalDateTime expirationDate;

    @Schema(description = "Order amount", example = "150.00")
    private BigDecimal orderAmount;

    @Schema(description = "Idempotency key", example = "order-123e4567-e89b-12d3-a456-426614174002")
    private String idempotencyKey;
}