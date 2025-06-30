package com.Ecommerce.Product_Service.Payload.Discont;


import com.Ecommerce.Product_Service.Entities.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppliedDiscountDTO {
    private UUID discountId;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private String description;
}