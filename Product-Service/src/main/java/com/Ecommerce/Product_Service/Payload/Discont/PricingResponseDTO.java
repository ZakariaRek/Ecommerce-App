package com.Ecommerce.Product_Service.Payload.Discont;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PricingResponseDTO {
    private UUID productId;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private BigDecimal totalDiscount;
    private BigDecimal discountPercentage;
    private List<AppliedDiscountDTO> appliedDiscounts;
    private boolean hasActiveDiscounts;
}
