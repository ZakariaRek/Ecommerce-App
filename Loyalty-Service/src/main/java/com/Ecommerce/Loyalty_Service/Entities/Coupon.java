package com.Ecommerce.Loyalty_Service.Entities;


import com.Ecommerce.Loyalty_Service.Listeners.CouponMongoListener;
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
@EntityListeners(CouponMongoListener.class)
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

    @Column(columnDefinition = "TEXT")
    private String excludeCategories; // JSON array of excluded category IDs

    @Column(columnDefinition = "TEXT")
    private String excludeProducts; // JSON array of excluded product IDs

    @Enumerated(EnumType.STRING)
    private MembershipTier minimumTier; // Minimum membership tier required

    @Column(nullable = false)
    private Integer usageCount = 0; // Track how many times used

    private Integer maxUsagePerUser; // Limit per user

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CouponScope appliesTo = CouponScope.ORDER; // ORDER, SHIPPING, SPECIFIC_PRODUCTS

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // NEW: Relationship to usage history
    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CouponUsageHistory> usageHistory = new ArrayList<>();

    // Helper methods
    public boolean isValidForUser(UUID userId) {
        if (maxUsagePerUser == null) return true;

        long userUsageCount = usageHistory.stream()
                .filter(usage -> usage.getUserId().equals(userId))
                .count();

        return userUsageCount < maxUsagePerUser;
    }

    public boolean isActive() {
        return !isUsed &&
                expirationDate.isAfter(LocalDateTime.now()) &&
                (usageLimit == 0 || usageCount < usageLimit);
    }
}