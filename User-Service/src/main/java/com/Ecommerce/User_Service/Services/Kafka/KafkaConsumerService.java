package com.Ecommerce.User_Service.Services.Kafka;

import com.Ecommerce.User_Service.Events.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    // Example consumer method - you can customize to meet specific requirements
    @KafkaListener(
            topics = "${User-service.kafka.topics.user-created}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consumeUserCreatedEvent(UserEvent event) {
        logger.info("Received user created event: {}", event);
        // Process the event as needed
    }

    // Consumer for external events from other services if needed
    // For example, from Order Service when an order is created for a user
    /*
    @KafkaListener(
            topics = "order-created",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderCreatedEvent(OrderEvent event) {
        logger.info("Received order created event: {}", event);
        // Process the event, perhaps update user's order history
    }
    */
}