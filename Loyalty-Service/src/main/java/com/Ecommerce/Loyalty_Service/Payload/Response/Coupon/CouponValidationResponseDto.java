package com.Ecommerce.Loyalty_Service.Payload.Response.Coupon;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Coupon validation result")
public class CouponValidationResponseDto {

    @Schema(description = "Whether coupon is valid", example = "true")
    private boolean isValid;

    @Schema(description = "Coupon code that was validated", example = "SAVE20OFF")
    private String couponCode;

    @Schema(description = "Validation message", example = "Coupon is valid and ready to use")
    private String message;

    @Schema(description = "Expected discount amount if applied", example = "20.00")
    private BigDecimal expectedDiscount;
}