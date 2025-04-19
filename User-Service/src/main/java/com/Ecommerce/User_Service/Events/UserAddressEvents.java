package com.Ecommerce.User_Service.Events;


import com.Ecommerce.User_Service.Models.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserAddressEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAddressCreatedEvent {
        private String addressId;
        private String userId;
        private AddressType addressType;
        private String street;
        private String city;
        private String state;
        private String country;
        private String zipCode;
        private boolean isDefault;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAddressUpdatedEvent {
        private String addressId;
        private String userId;
        private AddressType addressType;
        private String street;
        private String city;
        private String state;
        private String country;
        private String zipCode;
        private boolean isDefault;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAddressDeletedEvent {
        private String addressId;
        private String userId;
        private AddressType addressType;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDefaultAddressChangedEvent {
        private String addressId;
        private String userId;
        private AddressType addressType;
        private String previousDefaultAddressId;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserAddressTypeChangedEvent {
        private String addressId;
        private String userId;
        private AddressType previousAddressType;
        private AddressType newAddressType;
        private LocalDateTime updatedAt;
    }
}