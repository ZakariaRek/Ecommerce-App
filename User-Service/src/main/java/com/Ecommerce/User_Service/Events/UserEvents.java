package com.Ecommerce.User_Service.Events;


import com.Ecommerce.User_Service.Models.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

public class UserEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCreatedEvent {
        private String userId;
        private String username;
        private String email;
        private UserStatus status;
        private Set<String> roles;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserUpdatedEvent {
        private String userId;
        private String username;
        private String email;
        private UserStatus status;
        private Set<String> roles;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDeletedEvent {
        private String userId;
        private String username;
        private String email;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusChangedEvent {
        private String userId;
        private String username;
        private UserStatus previousStatus;
        private UserStatus newStatus;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleChangedEvent {
        private String userId;
        private String username;
        private Set<String> previousRoles;
        private Set<String> newRoles;
        private LocalDateTime updatedAt;
    }
}