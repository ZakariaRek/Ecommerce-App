package com.Ecommerce.Loyalty_Service.Payload.Request.Coupon;
import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
@Schema(description = "Request to generate a new coupon")
public class CouponGenerateRequestDto {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID for whom the coupon is generated", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @NotNull(message = "Discount type is required")
    @Schema(description = "Type of discount")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Schema(description = "Discount value (percentage or fixed amount)", example = "15.00")
    private BigDecimal discountValue;

    @NotNull(message = "Minimum purchase amount is required")
    @DecimalMin(value = "0.00", message = "Minimum purchase amount cannot be negative")
    @Schema(description = "Minimum purchase amount to use coupon", example = "50.00")
    private BigDecimal minPurchaseAmount;

    @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than 0")
    @Schema(description = "Maximum discount amount (optional)", example = "100.00")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    @Schema(description = "Coupon expiration date")
    private LocalDateTime expirationDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    @Schema(description = "Number of times coupon can be used", example = "1")
    private int usageLimit = 1;
}
