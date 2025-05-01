package com.Ecommerce.Notification_Service.Services.Kafka;

import com.Ecommerce.Notification_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Notification_Service.Events.NotificationEvents;
import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending Notification events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when a notification is created
     */
    public void publishNotificationCreated(Notification notification) {
        NotificationEvents.NotificationCreatedEvent event = new NotificationEvents.NotificationCreatedEvent(notification);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_NOTIFICATION_CREATED, notification.getUserId().toString(), event);
        log.info("Published notification created event: {}", event);
    }

    /**
     * Publish an event when a notification is read
     */
    public void publishNotificationRead(Notification notification) {
        NotificationEvents.NotificationReadEvent event = new NotificationEvents.NotificationReadEvent(notification);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_NOTIFICATION_READ, notification.getUserId().toString(), event);
        log.info("Published notification read event: {}", event);
    }

    /**
     * Publish an event when a notification is deleted
     */
    public void publishNotificationDeleted(Notification notification, String deletionReason) {
        NotificationEvents.NotificationDeletedEvent event = new NotificationEvents.NotificationDeletedEvent(notification, deletionReason);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_NOTIFICATION_DELETED, notification.getUserId().toString(), event);
        log.info("Published notification deleted event: {}", event);
    }

    /**
     * Publish an event when bulk notifications are sent
     */
    public void publishBulkNotificationSent(NotificationType type, String content, int recipientCount) {
        NotificationEvents.BulkNotificationSentEvent event = new NotificationEvents.BulkNotificationSentEvent(type, content, recipientCount);
        kafkaTemplate.send(KafkaProducerConfig.TOPIC_NOTIFICATION_CREATED, "bulk", event);
        log.info("Published bulk notification sent event: {}", event);
    }
}