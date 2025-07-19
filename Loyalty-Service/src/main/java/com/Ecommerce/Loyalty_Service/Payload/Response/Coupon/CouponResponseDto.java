package com.Ecommerce.Loyalty_Service.Payload.Response.Coupon;


import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
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
@Schema(description = "Coupon information response")
public class CouponResponseDto {

    @Schema(description = "Coupon ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Coupon code", example = "SAVE20OFF")
    private String code;

    @Schema(description = "Type of discount")
    private DiscountType discountType;

    @Schema(description = "Discount value", example = "20.00")
    private BigDecimal discountValue;

    @Schema(description = "Minimum purchase amount", example = "100.00")
    private BigDecimal minPurchaseAmount;

    @Schema(description = "Maximum discount amount", example = "50.00")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "Coupon expiration date")
    private LocalDateTime expirationDate;

    @Schema(description = "User ID who owns the coupon", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "Whether coupon has been used", example = "false")
    private boolean isUsed;

    @Schema(description = "Usage limit", example = "1")
    private int usageLimit;

    @Schema(description = "Whether coupon can be stacked with others", example = "true")
    private Boolean stackable;

    @Schema(description = "Priority level for stacking", example = "1")
    private Integer priorityLevel;

    @Schema(description = "Coupon creation timestamp")
    private LocalDateTime createdAt;
}
