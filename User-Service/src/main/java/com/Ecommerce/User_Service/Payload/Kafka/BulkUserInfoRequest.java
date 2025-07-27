package com.Ecommerce.User_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for fetching multiple user information from User Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserInfoRequest {
    private String requestId;
    private List<UUID> userIds;
    private String requestingService;
    private LocalDateTime requestTime;
    private String purpose; // "BULK_NOTIFICATION", "PROMOTION", etc.
    private boolean includeAddresses; // Whether to include user addresses
    private boolean includeRoles; // Whether to include user roles

    public static BulkUserInfoRequest create(List<UUID> userIds, String purpose) {
        return BulkUserInfoRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userIds(userIds)
                .requestingService("notification-service")
                .requestTime(LocalDateTime.now())
                .purpose(purpose)
                .includeAddresses(true)
                .includeRoles(false)
                .build();
    }

    public static BulkUserInfoRequest create(List<UUID> userIds, String purpose, boolean includeAddresses, boolean includeRoles) {
        return BulkUserInfoRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userIds(userIds)
                .requestingService("notification-service")
                .requestTime(LocalDateTime.now())
                .purpose(purpose)
                .includeAddresses(includeAddresses)
                .includeRoles(includeRoles)
                .build();
    }
}
