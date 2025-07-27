package com.Ecommerce.Notification_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO from User Service with multiple user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserInfoResponse {
    private String requestId;
    private List<UserInfoResponse> users;
    private int totalRequested;
    private int totalFound;
    private LocalDateTime responseTime;
    private String status; // "SUCCESS", "PARTIAL_SUCCESS", "FAILED"

    public static BulkUserInfoResponse create(String requestId, List<UserInfoResponse> users) {
        int totalRequested = users.size();
        int totalFound = (int) users.stream().filter(u -> "SUCCESS".equals(u.getStatus_response())).count();

        String status = totalFound == 0 ? "FAILED" :
                totalFound == totalRequested ? "SUCCESS" : "PARTIAL_SUCCESS";

        return BulkUserInfoResponse.builder()
                .requestId(requestId)
                .users(users)
                .totalRequested(totalRequested)
                .totalFound(totalFound)
                .responseTime(LocalDateTime.now())
                .status(status)
                .build();
    }

    /**
     * Convert to map for easy lookup by user ID
     */
    public Map<UUID, UserInfoResponse> toUserMap() {
        return users.stream()
                .filter(user -> "SUCCESS".equals(user.getStatus_response()))
                .collect(Collectors.toMap(UserInfoResponse::getUserId, user -> user));
    }

    /**
     * Get emails only from successful responses
     */
    public Map<UUID, String> getEmailMap() {
        return users.stream()
                .filter(user -> "SUCCESS".equals(user.getStatus_response()) && user.getEmail() != null)
                .collect(Collectors.toMap(UserInfoResponse::getUserId, UserInfoResponse::getEmail));
    }

    /**
     * Get verified emails only
     */
    public Map<UUID, String> getVerifiedEmailMap() {
        return users.stream()
                .filter(user -> "SUCCESS".equals(user.getStatus_response()) &&
                        user.getEmail() != null &&
                        user.isEmailVerified())
                .collect(Collectors.toMap(UserInfoResponse::getUserId, UserInfoResponse::getEmail));
    }

    /**
     * Get marketing opt-in emails only
     */
    public Map<UUID, String> getMarketingEmailMap() {
        return users.stream()
                .filter(user -> "SUCCESS".equals(user.getStatus_response()) &&
                        user.getEmail() != null &&
                        user.isEmailVerified() &&
                        user.isMarketingOptIn())
                .collect(Collectors.toMap(UserInfoResponse::getUserId, UserInfoResponse::getEmail));
    }

    /**
     * Get users with specific role
     */
    public List<UserInfoResponse> getUsersWithRole(String roleName) {
        return users.stream()
                .filter(user -> "SUCCESS".equals(user.getStatus_response()) && user.hasRole(roleName))
                .collect(Collectors.toList());
    }

    /**
     * Get admin users
     */
    public List<UserInfoResponse> getAdminUsers() {
        return getUsersWithRole("ROLE_ADMIN");
    }
}
