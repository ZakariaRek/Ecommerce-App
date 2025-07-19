package com.Ecommerce.Loyalty_Service.Entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal minPurchaseAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    private LocalDateTime expirationDate;

    private UUID userId;

    private boolean isUsed;

    private int usageLimit;

    // NEW STACKING SUPPORT FIELDS
    @Column(nullable = false)
    private Boolean stackable = true;

    @Column(nullable = false)
    private Integer priorityLevel = 1; // Lower number = higher priority

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


}