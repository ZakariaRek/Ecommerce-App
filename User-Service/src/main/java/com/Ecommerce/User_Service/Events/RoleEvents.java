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


}