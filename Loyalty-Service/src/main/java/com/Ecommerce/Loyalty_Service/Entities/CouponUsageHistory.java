package com.Ecommerce.Loyalty_Service.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupon_usage_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageHistory {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false)
    private UUID userId;

    private UUID orderId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @CreationTimestamp
    private LocalDateTime usedAt;
}
