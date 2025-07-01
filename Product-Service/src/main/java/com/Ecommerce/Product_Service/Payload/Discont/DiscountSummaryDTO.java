package com.Ecommerce.Product_Service.Payload.Discont;

import com.Ecommerce.Product_Service.Entities.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DiscountSummaryDTO {
    private UUID id;
    private UUID productId;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime endDate;
    private boolean isActive;
}