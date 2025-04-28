package com.Ecommerce.User_Service.Utils;

import com.Ecommerce.User_Service.Events.UserEvents;
import com.Ecommerce.User_Service.Models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for Kafka message handling
 */
public class KafkaUtils {
    private static final Logger logger = LoggerFactory.getLogger(KafkaUtils.class);

    /**
     * Creates a UserCreatedEvent from a User entity
     */
    public static UserEvents.UserCreatedEvent createUserCreatedEvent(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserEvents.UserCreatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .status(user.getStatus())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a UserUpdatedEvent from a User entity
     */
    public static UserEvents.UserUpdatedEvent createUserUpdatedEvent(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserEvents.UserUpdatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .status(user.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a UserDeletedEvent from a User entity
     */
    public static UserEvents.UserDeletedEvent createUserDeletedEvent(User user) {
        return UserEvents.UserDeletedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .deletedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a UserStatusChangedEvent from current and previous User states
     */
    public static UserEvents.UserStatusChangedEvent createUserStatusChangedEvent(User user, User previousUser) {
        return UserEvents.UserStatusChangedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .previousStatus(previousUser.getStatus())
                .newStatus(user.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a UserRoleChangedEvent from a User entity and previous roles
     */
    public static UserEvents.UserRoleChangedEvent createUserRoleChangedEvent(User user, Set<String> previousRoles) {
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

    /**
     * Creates a Kafka message with the provided key and payload
     */
    public static <T> Message<T> createMessage(String key, T payload) {
        return MessageBuilder
                .withPayload(payload)
                .setHeader(KafkaHeaders.KEY, key)
                .build();
    }

    /**
     * Logs Kafka message details
     */
    public static void logKafkaMessage(String topic, String key, Object payload) {
        logger.info("Sending message to topic: {}, key: {}, payload: {}", topic, key, payload);
    }
}