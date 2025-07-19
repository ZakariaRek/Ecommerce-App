package com.Ecommerce.Loyalty_Service.Payload.Request.Coupon;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
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
public class CouponPointsPurchaseRequestDto {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Discount type is required")
    private com.Ecommerce.Loyalty_Service.Entities.DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @Min(value = 1, message = "Points cost must be at least 1")
    private int pointsCost;

    @NotNull(message = "Minimum purchase amount is required")
    @DecimalMin(value = "0.00", message = "Minimum purchase amount cannot be negative")
    private BigDecimal minPurchaseAmount;

    @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than 0")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private java.time.LocalDateTime expirationDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private int usageLimit = 1;
}
