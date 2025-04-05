package com.Ecommerce.Loyalty_Service.Entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crm")
@Data
public class CRM {
    @Id
    private UUID id;
    private UUID userId;
    private int totalPoints;

    @Enumerated(EnumType.STRING)
    private MembershipTier membershipLevel;

    private LocalDateTime joinDate;
    private LocalDateTime lastActivity;

}
