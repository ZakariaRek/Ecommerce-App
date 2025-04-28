package com.Ecommerce.Loyalty_Service.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event representing a user's membership tier change
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierChangedEvent {
    private UUID userId;
    private String previousTier;
    private String newTier;
    private LocalDateTime changedAt;
}
