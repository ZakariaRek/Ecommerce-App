package com.Ecommerce.Loyalty_Service.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event representing points being awarded to a user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsAwardedEvent {
    private UUID userId;
    private int points;
    private String source;
    private LocalDateTime awardedAt;
    private int newBalance;
}
