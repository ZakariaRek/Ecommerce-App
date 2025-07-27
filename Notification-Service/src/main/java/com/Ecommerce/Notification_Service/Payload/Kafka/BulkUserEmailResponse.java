package com.Ecommerce.Notification_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO from User Service with multiple user emails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserEmailResponse {
    private String requestId;
    private List<UserEmailResponse> users;
    private int totalRequested;
    private int totalFound;
    private LocalDateTime responseTime;
    private String status; // "SUCCESS", "PARTIAL_SUCCESS", "FAILED"

    public static BulkUserEmailResponse create(String requestId, List<UserEmailResponse> users) {
        int totalRequested = users.size();
        int totalFound = (int) users.stream().filter(u -> "SUCCESS".equals(u.getStatus())).count();

        String status = totalFound == 0 ? "FAILED" :
                totalFound == totalRequested ? "SUCCESS" : "PARTIAL_SUCCESS";

        return BulkUserEmailResponse.builder()
                .requestId(requestId)
                .users(users)
                .totalRequested(totalRequested)
                .totalFound(totalFound)
                .responseTime(LocalDateTime.now())
                .status(status)
                .build();
    }
}
