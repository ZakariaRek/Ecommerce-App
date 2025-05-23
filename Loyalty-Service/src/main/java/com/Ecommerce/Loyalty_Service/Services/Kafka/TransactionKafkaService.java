package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.Config.KafkaConfig;
import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Events.LoyaltyEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending Transaction events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when a transaction is recorded
     */
    public void publishTransactionRecorded(PointTransaction transaction) {
        LoyaltyEvents.TransactionRecordedEvent event = new LoyaltyEvents.TransactionRecordedEvent(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getType(),
                transaction.getPoints(),
                transaction.getSource(),
                transaction.getBalance()
        );

        // Determine the appropriate topic based on transaction type
        String topic;
        switch (transaction.getType()) {
            case EARN:
                topic = KafkaConfig.TOPIC_POINTS_EARNED;
                break;
            case REDEEM:
                topic = KafkaConfig.TOPIC_POINTS_REDEEMED;
                break;
            case EXPIRE:
                topic = KafkaConfig.TOPIC_POINTS_EXPIRED;
                break;
            case ADJUST:
                topic = KafkaConfig.TOPIC_POINTS_ADJUSTED;
                break;
            default:
                // Use points earned as default
                topic = KafkaConfig.TOPIC_POINTS_EARNED;
        }

        kafkaTemplate.send(topic, transaction.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published transaction recorded event: {}", event);
                    } else {
                        log.error("Failed to publish transaction recorded event: {}", event, ex);
                    }
                });
    }
}