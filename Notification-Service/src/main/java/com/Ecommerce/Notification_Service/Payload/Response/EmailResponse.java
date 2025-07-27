package com.Ecommerce.Notification_Service.Payload.Response;


import lombok.Data;

/**
 * Generic email response DTO
 */
@Data
public class EmailResponse {
    private boolean success;
    private String message;
    private int successCount;
    private int failureCount;
    private String timestamp;

    public static EmailResponse success(String message) {
        EmailResponse response = new EmailResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        return response;
    }

    public static EmailResponse failure(String message) {
        EmailResponse response = new EmailResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        return response;
    }

    public static EmailResponse bulk(int successCount, int failureCount) {
        EmailResponse response = new EmailResponse();
        response.setSuccess(failureCount == 0);
        response.setSuccessCount(successCount);
        response.setFailureCount(failureCount);
        response.setMessage(String.format("Bulk email completed. Success: %d, Failures: %d", successCount, failureCount));
        response.setTimestamp(java.time.LocalDateTime.now().toString());
        return response;
    }
}