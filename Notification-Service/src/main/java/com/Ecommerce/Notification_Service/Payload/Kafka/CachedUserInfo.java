package com.Ecommerce.Notification_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Internal DTO for caching complete user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUserInfo {
    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
    private Set<String> roles;
    private List<CachedUserAddress> addresses;
    private CachedUserAddress defaultAddress;
    private String preferredLanguage;
    private boolean emailVerified;
    private boolean marketingOptIn;
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public static CachedUserInfo fromResponse(UserInfoResponse response) {
        List<CachedUserAddress> cachedAddresses = null;
        if (response.getAddresses() != null) {
            cachedAddresses = response.getAddresses().stream()
                    .map(CachedUserAddress::fromUserAddressDTO)
                    .toList();
        }

        CachedUserAddress cachedDefaultAddress = null;
        if (response.getDefaultAddress() != null) {
            cachedDefaultAddress = CachedUserAddress.fromUserAddressDTO(response.getDefaultAddress());
        }

        return CachedUserInfo.builder()
                .userId(response.getUserId())
                .username(response.getUsername())
                .email(response.getEmail())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .status(response.getStatus() != null ? response.getStatus() : "ACTIVE")
                .roles(response.getRoles())
                .addresses(cachedAddresses)
                .defaultAddress(cachedDefaultAddress)
                .preferredLanguage(response.getPreferredLanguage())
                .emailVerified(response.isEmailVerified())
                .marketingOptIn(response.isMarketingOptIn())
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1)) // Cache for 1 hour
                .build();
    }

    public UserInfoResponse toUserInfoResponse(String requestId) {
        List<UserInfoResponse.UserAddressDTO> addressDTOs = null;
        if (addresses != null) {
            addressDTOs = addresses.stream()
                    .map(CachedUserAddress::toUserAddressDTO)
                    .toList();
        }

        UserInfoResponse.UserAddressDTO defaultAddressDTO = null;
        if (defaultAddress != null) {
            defaultAddressDTO = defaultAddress.toUserAddressDTO();
        }

        return UserInfoResponse.builder()
                .requestId(requestId)
                .userId(userId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .roles(roles)
                .addresses(addressDTOs)
                .defaultAddress(defaultAddressDTO)
                .preferredLanguage(preferredLanguage)
                .emailVerified(emailVerified)
                .marketingOptIn(marketingOptIn)
                .responseTime(LocalDateTime.now())
                .status_response("SUCCESS")
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedUserAddress {
        private String id;
        private String addressType;
        private String street;
        private String city;
        private String state;
        private String country;
        private String zipCode;
        private boolean isDefault;

        public static CachedUserAddress fromUserAddressDTO(UserInfoResponse.UserAddressDTO addressDTO) {
            return CachedUserAddress.builder()
                    .id(addressDTO.getId())
                    .addressType(addressDTO.getAddressType())
                    .street(addressDTO.getStreet())
                    .city(addressDTO.getCity())
                    .state(addressDTO.getState())
                    .country(addressDTO.getCountry())
                    .zipCode(addressDTO.getZipCode())
                    .isDefault(addressDTO.isDefault())
                    .build();
        }

        public UserInfoResponse.UserAddressDTO toUserAddressDTO() {
            return UserInfoResponse.UserAddressDTO.builder()
                    .id(id)
                    .addressType(addressType)
                    .street(street)
                    .city(city)
                    .state(state)
                    .country(country)
                    .zipCode(zipCode)
                    .isDefault(isDefault)
                    .build();
        }
    }
}