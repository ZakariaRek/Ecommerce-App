package com.Ecommerce.Loyalty_Service.Payload.Kafka;


import com.Ecommerce.Loyalty_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCalculationContext {
    private DiscountCalculationRequest originalRequest;
    private BigDecimal productDiscount;
    private BigDecimal orderDiscount;
    private BigDecimal couponDiscount;
    private BigDecimal amountAfterOrderDiscount;
    private BigDecimal amountAfterCouponDiscount;
}