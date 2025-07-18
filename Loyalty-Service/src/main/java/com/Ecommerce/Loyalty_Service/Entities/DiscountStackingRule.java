package com.Ecommerce.Loyalty_Service.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discount_stacking_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountStackingRule {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiscountType primaryDiscountType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stackableWith; // JSON array of compatible discount types

    @Column(precision = 5, scale = 2)
    private BigDecimal maxTotalDiscountPercent;

    @Column(nullable = false)
    private Integer priorityOrder;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}