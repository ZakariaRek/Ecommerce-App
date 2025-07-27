package com.Ecommerce.User_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
}
