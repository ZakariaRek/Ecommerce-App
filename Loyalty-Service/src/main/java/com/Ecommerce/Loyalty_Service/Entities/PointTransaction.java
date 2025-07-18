package com.Ecommerce.Loyalty_Service.Entities;

import com.Ecommerce.Loyalty_Service.Listeners.PointTransactionMongoListener;
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
@EntityListeners(PointTransactionMongoListener.class)
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

    @Column(columnDefinition = "TEXT")
    private String transactionMetadata; // JSON for additional context

    private LocalDateTime expirationDate; // When points expire

    @Column(precision = 10, scale = 2)
    private BigDecimal orderAmount; // Related order amount for context

    @Column(length = 20)
    private String campaignCode; // If points were earned through a campaign
}