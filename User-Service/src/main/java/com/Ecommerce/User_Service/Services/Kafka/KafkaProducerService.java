package com.Ecommerce.User_Service.Services.Kafka;

import com.Ecommerce.User_Service.Events.UserEvent;
import com.Ecommerce.User_Service.Models.User;
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
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Value("${User-service.kafka.topics.user-created}")
    private String userCreatedTopic;

    @Value("${User-service.kafka.topics.user-updated}")
    private String userUpdatedTopic;

    @Value("${User-service.kafka.topics.user-deleted}")
    private String userDeletedTopic;

    @Value("${User-service.kafka.topics.user-status-changed}")
    private String userStatusChangedTopic;

    public void sendUserCreatedEvent(User user) {
        UserEvent event = createUserEvent(user, UserEvent.EventType.CREATED);
        send(userCreatedTopic, user.getId(), event);
    }

    public void sendUserUpdatedEvent(User user) {
        UserEvent event = createUserEvent(user, UserEvent.EventType.UPDATED);
        send(userUpdatedTopic, user.getId(), event);
    }

    public void sendUserDeletedEvent(User user) {
        UserEvent event = createUserEvent(user, UserEvent.EventType.DELETED);
        send(userDeletedTopic, user.getId(), event);
    }

    public void sendUserStatusChangedEvent(User user) {
        UserEvent event = createUserEvent(user, UserEvent.EventType.STATUS_CHANGED);
        send(userStatusChangedTopic, user.getId(), event);
    }

    private UserEvent createUserEvent(User user, UserEvent.EventType eventType) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return new UserEvent(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                user.getStatus(),
                eventType,
                LocalDateTime.now()
        );
    }

    private void send(String topic, String key, UserEvent event) {
        try {
            CompletableFuture<SendResult<String, UserEvent>> future = kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Sent event [{}] with key={} to topic={}, partition={}, offset={}",
                            event.getEventType(),
                            key,
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    logger.error("Unable to send event [{}] with key={} to topic={} due to : {}",
                            event.getEventType(),
                            key,
                            topic,
                            ex.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Error sending event to Kafka: {}", e.getMessage(), e);
        }
    }
}