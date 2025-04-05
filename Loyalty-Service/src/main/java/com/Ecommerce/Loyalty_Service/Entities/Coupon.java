package com.Ecommerce.Loyalty_Service.Entities;


import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Data
public class Coupon {
    @Id
    private UUID id;
    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal discountValue;
    private BigDecimal minPurchaseAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime expirationDate;
    private UUID userId;
    private boolean isUsed;
    private int usageLimit;
}
