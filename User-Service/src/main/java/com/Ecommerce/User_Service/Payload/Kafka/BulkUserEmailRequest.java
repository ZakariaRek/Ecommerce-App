package com.Ecommerce.User_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for fetching multiple user emails from User Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserEmailRequest {
    private String requestId;
    private List<UUID> userIds;
    private String requestingService;
    private LocalDateTime requestTime;
    private String purpose; // "BULK_EMAIL_NOTIFICATION", "PROMOTION", etc.

    public static BulkUserEmailRequest create(List<UUID> userIds, String purpose) {
        return BulkUserEmailRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userIds(userIds)
                .requestingService("notification-service")
                .requestTime(LocalDateTime.now())
                .purpose(purpose)
                .build();
    }
}
