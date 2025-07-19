package com.Ecommerce.Loyalty_Service.Payload.Request.Transaction;

import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Request to get transaction history with filters")
public class TransactionHistoryRequestDto {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "Filter by transaction type (optional)")
    private TransactionType type;

    @Schema(description = "Start date for filtering (optional)")
    private LocalDateTime startDate;

    @Schema(description = "End date for filtering (optional)")
    private LocalDateTime endDate;

    @Min(value = 0, message = "Page number cannot be negative")
    @Schema(description = "Page number for pagination", example = "0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Schema(description = "Page size for pagination", example = "20")
    private int size = 20;
}
