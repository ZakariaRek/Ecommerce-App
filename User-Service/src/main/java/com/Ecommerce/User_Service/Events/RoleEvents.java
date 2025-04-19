package com.Ecommerce.User_Service.Events;

import com.Ecommerce.User_Service.Models.ERole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class RoleEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleCreatedEvent {
        private String roleId;
        private ERole roleName;
        private String description;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleUpdatedEvent {
        private String roleId;
        private ERole roleName;
        private String description;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDeletedEvent {
        private String roleId;
        private ERole roleName;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleAssignedToUserEvent {
        private String roleId;
        private ERole roleName;
        private String userId;
        private String username;
        private LocalDateTime assignedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleRemovedFromUserEvent {
        private String roleId;
        private ERole roleName;
        private String userId;
        private String username;
        private LocalDateTime removedAt;
    }
}