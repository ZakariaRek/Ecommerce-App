// File: Notification-Service/src/main/java/com/Ecommerce/Notification_Service/Payload/Kafka/UserInfoRequest.java
package com.Ecommerce.Notification_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for fetching complete user information from User Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoRequest {
    private String requestId;
    private UUID userId;
    private String requestingService;
    private LocalDateTime requestTime;
    private String purpose; // "NOTIFICATION", "ORDER_PROCESSING", etc.
    private boolean includeAddresses; // Whether to include user addresses
    private boolean includeRoles; // Whether to include user roles

    public static UserInfoRequest create(UUID userId, String purpose) {
        return UserInfoRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userId(userId)
                .requestingService("notification-service")
                .requestTime(LocalDateTime.now())
                .purpose(purpose)
                .includeAddresses(true)
                .includeRoles(false)
                .build();
    }

    public static UserInfoRequest create(UUID userId, String purpose, boolean includeAddresses, boolean includeRoles) {
        return UserInfoRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userId(userId)
                .requestingService("notification-service")
                .requestTime(LocalDateTime.now())
                .purpose(purpose)
                .includeAddresses(includeAddresses)
                .includeRoles(includeRoles)
                .build();
    }
}



