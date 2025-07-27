package com.Ecommerce.Notification_Service.Payload.Kafka;

import com.Ecommerce.Notification_Service.Models.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO from User Service with complete user information
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String requestId;
    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String status; // UserStatus as string
    private Set<String> roles;
    private List<UserAddressDTO> addresses;
    private UserAddressDTO defaultAddress;
    private String preferredLanguage;
    private boolean emailVerified;
    private boolean marketingOptIn;
    private LocalDateTime responseTime;
    private String status_response; // "SUCCESS", "USER_NOT_FOUND", "ERROR"
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAddressDTO {
        private String id;
        private String addressType;
        private String street;
        private String city;
        private String state;
        private String country;
        private String zipCode;
        private boolean isDefault;
    }

    public static UserInfoResponse success(String requestId, UUID userId, String username, String email) {
        return UserInfoResponse.builder()
                .requestId(requestId)
                .userId(userId)
                .username(username)
                .email(email)
                .emailVerified(true)
                .marketingOptIn(true)
                .responseTime(LocalDateTime.now())
                .status_response("SUCCESS")
                .build();
    }

    public static UserInfoResponse notFound(String requestId, UUID userId) {
        return UserInfoResponse.builder()
                .requestId(requestId)
                .userId(userId)
                .responseTime(LocalDateTime.now())
                .status_response("USER_NOT_FOUND")
                .errorMessage("User not found")
                .build();
    }

    public static UserInfoResponse error(String requestId, UUID userId, String errorMessage) {
        return UserInfoResponse.builder()
                .requestId(requestId)
                .userId(userId)
                .responseTime(LocalDateTime.now())
                .status_response("ERROR")
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Get formatted address string for notifications
     */
    public String getFormattedDefaultAddress() {
        if (defaultAddress == null) {
            return "No address on file";
        }

        StringBuilder address = new StringBuilder();
        if (defaultAddress.getStreet() != null) {
            address.append(defaultAddress.getStreet());
        }
        if (defaultAddress.getCity() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(defaultAddress.getCity());
        }
        if (defaultAddress.getState() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(defaultAddress.getState());
        }
        if (defaultAddress.getZipCode() != null) {
            if (address.length() > 0) address.append(" ");
            address.append(defaultAddress.getZipCode());
        }

        return address.toString();
    }

    /**
     * Get full display name
     */
    public String getFullName() {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName.trim());
        }

        return name.length() > 0 ? name.toString() : username;
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Check if user can receive notification type based on preferences
     */
    public boolean canReceiveNotification(NotificationType notificationType) {
        // Basic implementation - can be enhanced with actual user preferences
        if (!emailVerified) {
            // Only allow critical notifications for unverified emails
            return notificationType == NotificationType.PAYMENT_CONFIRMATION ||
                    notificationType == NotificationType.ORDER_STATUS ||
                    notificationType == NotificationType.ACCOUNT_ACTIVITY;
        }

        if (!marketingOptIn) {
            // No promotional notifications for users who opted out
            return notificationType != NotificationType.PROMOTION &&
                    notificationType != NotificationType.DISCOUNT_ACTIVATED;
        }

        return true;
    }
}
