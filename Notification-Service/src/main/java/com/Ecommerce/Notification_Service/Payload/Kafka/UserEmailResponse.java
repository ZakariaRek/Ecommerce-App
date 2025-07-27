package com.Ecommerce.Notification_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO from User Service with user email
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailResponse {
    private String requestId;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String preferredLanguage;
    private boolean emailVerified;
    private boolean marketingOptIn;
    private LocalDateTime responseTime;
    private String status; // "SUCCESS", "USER_NOT_FOUND", "EMAIL_NOT_AVAILABLE"
    private String errorMessage;

    public static UserEmailResponse success(String requestId, UUID userId, String email) {
        return UserEmailResponse.builder()
                .requestId(requestId)
                .userId(userId)
                .email(email)
                .emailVerified(true)
                .marketingOptIn(true)
                .responseTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
    }

    public static UserEmailResponse notFound(String requestId, UUID userId) {
        return UserEmailResponse.builder()
                .requestId(requestId)
                .userId(userId)
                .responseTime(LocalDateTime.now())
                .status("USER_NOT_FOUND")
                .errorMessage("User not found")
                .build();
    }

    public static UserEmailResponse error(String requestId, UUID userId, String errorMessage) {
        return UserEmailResponse.builder()
                .requestId(requestId)
                .userId(userId)
                .responseTime(LocalDateTime.now())
                .status("ERROR")
                .errorMessage(errorMessage)
                .build();
    }
}
