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
@Schema(description = "Coupon application result")
public class CouponApplyResponseDto {

    @Schema(description = "Original purchase amount", example = "150.00")
    private BigDecimal originalAmount;

    @Schema(description = "Discount amount applied", example = "20.00")
    private BigDecimal discountAmount;

    @Schema(description = "Final amount after discount", example = "130.00")
    private BigDecimal finalAmount;

    @Schema(description = "Coupon code that was applied", example = "SAVE20OFF")
    private String couponCode;

    @Schema(description = "Success message", example = "Coupon applied successfully")
    private String message;
}