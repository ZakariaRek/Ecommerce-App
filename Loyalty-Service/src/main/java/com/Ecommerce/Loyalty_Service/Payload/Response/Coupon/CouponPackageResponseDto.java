package com.Ecommerce.Loyalty_Service.Payload.Response.Coupon;

import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponPackageResponseDto {
    private String packageName;
    private int pointsCost;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minPurchaseAmount;
    private BigDecimal maxDiscountAmount;
    private String description;
}