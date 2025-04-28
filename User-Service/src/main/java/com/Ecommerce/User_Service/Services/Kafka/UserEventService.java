package com.Ecommerce.User_Service.Services.Kafka;

import com.Ecommerce.User_Service.Config.KafkaConfig;
import com.Ecommerce.User_Service.Events.UserEvents;
import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreatedEvent(User user) {
        try {
            UserEvents.UserCreatedEvent event = UserEvents.UserCreatedEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .status(user.getStatus())
                    .roles(user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.toSet()))
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user created event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_CREATED, user.getId(), event);
            log.info("User created event published successfully for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user created event", e);
        }
    }

    public void publishUserUpdatedEvent(User user) {
        try {
            UserEvents.UserUpdatedEvent event = UserEvents.UserUpdatedEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .status(user.getStatus())
                    .roles(user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.toSet()))
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user updated event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_UPDATED, user.getId(), event);
            log.info("User updated event published successfully for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user updated event", e);
        }
    }

    public void publishUserDeletedEvent(User user) {
        try {
            UserEvents.UserDeletedEvent event = UserEvents.UserDeletedEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user deleted event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_DELETED, user.getId(), event);
            log.info("User deleted event published successfully for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user deleted event", e);
        }
    }

    public void publishUserStatusChangedEvent(User user, UserStatus previousStatus) {
        try {
            UserEvents.UserStatusChangedEvent event = UserEvents.UserStatusChangedEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .previousStatus(previousStatus)
                    .newStatus(user.getStatus())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user status changed event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_STATUS_CHANGED, user.getId(), event);
            log.info("User status changed event published successfully for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user status changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user status changed event", e);
        }
    }

    public void publishUserRoleChangedEvent(User user, Set<String> previousRoles) {
        try {
            Set<String> currentRoles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet());

            UserEvents.UserRoleChangedEvent event = UserEvents.UserRoleChangedEvent.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .previousRoles(previousRoles)
                    .newRoles(currentRoles)
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user role changed event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_ROLE_CHANGED, user.getId(), event);
            log.info("User role changed event published successfully for user ID: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user role changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user role changed event", e);
        }
    }
}