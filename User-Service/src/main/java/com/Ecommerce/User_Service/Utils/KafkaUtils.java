package com.Ecommerce.User_Service.Utils;

import com.Ecommerce.User_Service.Events.UserEvents;
import com.Ecommerce.User_Service.Models.Role;
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
     * Creates a UserEvent from a User entity
     */
    public static UserEvents.UserCreatedEvent createUserEvent(User user, UserEvent.EventType eventType) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return new UserEvent.(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                user.getStatus(),
                eventType,
                LocalDateTime.now()
        );
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