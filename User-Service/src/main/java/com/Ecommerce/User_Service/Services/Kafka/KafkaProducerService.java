package com.Ecommerce.User_Service.Services.Kafka;

import com.Ecommerce.User_Service.Events.UserEvents;
import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${User-service.kafka.topics.user-created}")
    private String userCreatedTopic;

    @Value("${User-service.kafka.topics.user-updated}")
    private String userUpdatedTopic;

    @Value("${User-service.kafka.topics.user-deleted}")
    private String userDeletedTopic;

    @Value("${User-service.kafka.topics.user-status-changed}")
    private String userStatusChangedTopic;

    @Value("${User-service.kafka.topics.user-role-changed}")
    private String userRoleChangedTopic;

    public void sendUserCreatedEvent(User user) {
        UserEvents.UserCreatedEvent event = createUserCreatedEvent(user);
        send(userCreatedTopic, user.getId(), event);
    }

    public void sendUserUpdatedEvent(User user) {
        UserEvents.UserUpdatedEvent event = createUserUpdatedEvent(user);
        send(userUpdatedTopic, user.getId(), event);
    }

    public void sendUserDeletedEvent(User user) {
        UserEvents.UserDeletedEvent event = createUserDeletedEvent(user);
        send(userDeletedTopic, user.getId(), event);
    }

    public void sendUserStatusChangedEvent(User user, UserStatus UpdatedUserStatus) {
        UserEvents.UserStatusChangedEvent event = createUserStatusChangedEvent(user, UpdatedUserStatus);
        send(userStatusChangedTopic, user.getId(), event);
    }

    public void sendUserRoleChangedEvent(User user, Set<String> previousRoles) {
        UserEvents.UserRoleChangedEvent event = createUserRoleChangedEvent(user, previousRoles);
        send(userRoleChangedTopic, user.getId(), event);
    }

    private UserEvents.UserCreatedEvent createUserCreatedEvent(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserEvents.UserCreatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserEvents.UserUpdatedEvent createUserUpdatedEvent(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserEvents.UserUpdatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .roles(roles)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserEvents.UserDeletedEvent createUserDeletedEvent(User user) {
        return UserEvents.UserDeletedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .deletedAt(LocalDateTime.now())
                .build();
    }

    private UserEvents.UserStatusChangedEvent createUserStatusChangedEvent(User user, UserStatus
            UpdatedUserSatus) {
        return UserEvents.UserStatusChangedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .previousStatus(user.getStatus())
                .newStatus(UpdatedUserSatus)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserEvents.UserRoleChangedEvent createUserRoleChangedEvent(User user, Set<String> previousRoles) {
        Set<String> newRoles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserEvents.UserRoleChangedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .previousRoles(previousRoles)
                .newRoles(newRoles)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private <T> void send(String topic, String key, T event) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Sent event to topic={}, partition={}, offset={}, key={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key);
                } else {
                    logger.error("Unable to send event to topic={} with key={} due to : {}",
                            topic,
                            key,
                            ex.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Error sending event to Kafka: {}", e.getMessage(), e);
        }
    }
}