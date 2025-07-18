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
@Table(name = "tier_benefits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierBenefit {
    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BenefitType benefitType;

    @Column(columnDefinition = "TEXT")
    private String benefitConfig; // JSON configuration

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage; // For discount benefits

    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount; // Maximum discount amount

    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount; // Minimum order for benefit

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}