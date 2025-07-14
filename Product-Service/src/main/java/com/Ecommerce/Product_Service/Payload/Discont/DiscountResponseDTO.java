package com.Ecommerce.Product_Service.Payload.Discont;

import com.Ecommerce.Product_Service.Entities.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DiscountResponseDTO {
    private UUID id;
    private UUID productId;
    private String productName;
    private DiscountType discountType;
    private BigDecimal discountValue;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
    private BigDecimal minPurchaseAmount;
    private BigDecimal maxDiscountAmount;
    private boolean isActive;
}