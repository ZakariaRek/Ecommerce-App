package com.Ecommerce.Order_Service.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "discount_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountRule {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiscountRuleType ruleType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conditions; // JSON conditions

    @Column(nullable = false, columnDefinition = "TEXT")
    private String discountConfig; // JSON discount configuration

    @Column(nullable = false)
    private Boolean active = true;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}