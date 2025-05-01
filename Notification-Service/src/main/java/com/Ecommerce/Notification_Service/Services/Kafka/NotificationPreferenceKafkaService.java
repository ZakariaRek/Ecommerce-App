package com.Ecommerce.Notification_Service.Services.Kafka;

import com.Ecommerce.Notification_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Notification_Service.Events.NotificationPreferenceEvents;
import com.Ecommerce.Notification_Service.Models.NotificationPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for sending Notification Preference events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPreferenceKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when a notification preference is created
     */
    public void publishPreferenceCreated(NotificationPreference preference) {
        NotificationPreferenceEvents.PreferenceCreatedEvent event =
                new NotificationPreferenceEvents.PreferenceCreatedEvent(preference);

        kafkaTemplate.send(KafkaProducerConfig.TOPIC_PREFERENCE_CREATED,
                preference.getUserId().toString(), event);

        log.info("Published preference created event: {}", event);
    }

    /**
     * Publish an event when a notification preference is updated
     */
    public void publishPreferenceUpdated(NotificationPreference preference, boolean oldEnabled) {
        NotificationPreferenceEvents.PreferenceUpdatedEvent event =
                new NotificationPreferenceEvents.PreferenceUpdatedEvent(preference, oldEnabled);

        kafkaTemplate.send(KafkaProducerConfig.TOPIC_PREFERENCE_UPDATED,
                preference.getUserId().toString(), event);

        log.info("Published preference updated event: {}", event);
    }

    /**
     * Publish an event when a user opts out of all notifications
     */
    public void publishUserOptedOut(UUID userId, String reason) {
        NotificationPreferenceEvents.UserOptedOutEvent event =
                new NotificationPreferenceEvents.UserOptedOutEvent(userId, reason);

        kafkaTemplate.send(KafkaProducerConfig.TOPIC_PREFERENCE_UPDATED,
                userId.toString(), event);

        log.info("Published user opted out event: {}", event);
    }
}