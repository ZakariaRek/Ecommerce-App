package com.Ecommerce.User_Service.Payload.Kafka;

import com.Ecommerce.User_Service.Models.UserAddress;
import com.Ecommerce.User_Service.Models.UserStatus;
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
    private UserStatus status;
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

        public static UserAddressDTO fromUserAddress(UserAddress address) {
            return UserAddressDTO.builder()
                    .id(address.getId())
                    .addressType(address.getAddressType().name())
                    .street(address.getStreet())
                    .city(address.getCity())
                    .state(address.getState())
                    .country(address.getCountry())
                    .zipCode(address.getZipCode())
                    .isDefault(address.isDefault())
                    .build();
        }
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
}
