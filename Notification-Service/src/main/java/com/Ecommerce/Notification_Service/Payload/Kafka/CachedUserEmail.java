package com.Ecommerce.Notification_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Internal DTO for caching user emails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUserEmail {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
    private boolean marketingOptIn;
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public static CachedUserEmail fromResponse(UserEmailResponse response) {
        return CachedUserEmail.builder()
                .userId(response.getUserId())
                .email(response.getEmail())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .emailVerified(response.isEmailVerified())
                .marketingOptIn(response.isMarketingOptIn())
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1)) // Cache for 1 hour
                .build();
    }
}
