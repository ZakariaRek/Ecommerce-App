package com.Ecommerce.User_Service.Events;

import com.Ecommerce.User_Service.Models.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String id;
    private String username;
    private String email;
    private Set<String> roles;
    private UserStatus status;
    private EventType eventType;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED,
        STATUS_CHANGED
    }
}