package com.Ecommerce.Loyalty_Service.Payload.Request.Coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to validate a coupon")
public class CouponValidationRequestDto {

    @NotBlank(message = "Coupon code is required")
    @Schema(description = "Coupon code to validate", example = "SAVE20OFF")
    private String couponCode;

    @NotNull(message = "Purchase amount is required")
    @DecimalMin(value = "0.01", message = "Purchase amount must be greater than 0")
    @Schema(description = "Purchase amount to validate against", example = "150.00")
    private BigDecimal purchaseAmount;
}