package com.Ecommerce.Notification_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for fetching user email from User Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailRequest {
    private String requestId;
    private UUID userId;
    private String requestingService;
    private LocalDateTime requestTime;
    private String purpose; // "EMAIL_NOTIFICATION", "BULK_EMAIL", etc.

    public static UserEmailRequest create(UUID userId, String purpose) {
        return UserEmailRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userId(userId)
                .requestingService("notification-service")
                .requestTime(LocalDateTime.now())
                .purpose(purpose)
                .build();
    }
}

