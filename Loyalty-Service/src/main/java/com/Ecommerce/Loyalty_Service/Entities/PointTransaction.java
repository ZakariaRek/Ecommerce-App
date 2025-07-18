package com.Ecommerce.Loyalty_Service.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private int points;

    @CreationTimestamp
    private LocalDateTime transactionDate;

    private String source;

    private int balance;

    // NEW FIELDS FOR BETTER TRACKING
    private UUID relatedOrderId;

    private UUID relatedCouponId;

    private LocalDateTime expirationDate; // When points expire

    @Column(precision = 10, scale = 2)
    private BigDecimal orderAmount;

    @Version
    @Column(name = "version")
    private Long version ;
    // New field for idempotency
    private String idempotencyKey;

}