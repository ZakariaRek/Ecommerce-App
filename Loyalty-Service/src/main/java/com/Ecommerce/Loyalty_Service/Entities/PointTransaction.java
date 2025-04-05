package com.Ecommerce.Loyalty_Service.Entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "point_transactions")
@Data
public class PointTransaction {
    @Id
    private UUID id;
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private int points;
    private LocalDateTime transactionDate;
    private String source;
    private int balance;
}
