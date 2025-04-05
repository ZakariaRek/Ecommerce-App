package com.Ecommerce.Loyalty_Service.Entities;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "loyalty_rewards")
@Data
public class LoyaltyReward {
    @Id
    private UUID id;
    private String name;
    private String description;
    private int pointsCost;
    private boolean isActive;
    private int expiryDays;
}
