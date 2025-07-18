package com.Ecommerce.Loyalty_Service.Payload.Kafka.Response;
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
public class CouponDiscountDetail {
    private String couponCode;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal calculatedDiscount;


}