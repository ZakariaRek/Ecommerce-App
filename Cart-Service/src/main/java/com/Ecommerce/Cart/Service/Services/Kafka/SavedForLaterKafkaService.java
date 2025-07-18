package com.Ecommerce.Cart.Service.Services.Kafka;

import com.Ecommerce.Cart.Service.Config.KafkaProducerConfig;
import com.Ecommerce.Cart.Service.Events.SavedForLaterEvents;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for sending SavedForLater events to Kafka topics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SavedForLaterKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event when an item is saved for later
     */
    public void publishItemSavedForLater(SavedForLater savedItem) {
        SavedForLaterEvents.ItemSavedForLaterEvent event =
                new SavedForLaterEvents.ItemSavedForLaterEvent(savedItem);

        kafkaTemplate.send(KafkaProducerConfig.TOPIC_ITEM_SAVED_FOR_LATER,
                savedItem.getUserId().toString(), event);

        log.info("Published item saved for later event: {}", event);
    }



    /**
     * Publish an event when a saved item is removed
     */
    public void publishSavedItemRemoved(SavedForLater savedItem, String removalReason) {
        SavedForLaterEvents.SavedItemRemovedEvent event =
                new SavedForLaterEvents.SavedItemRemovedEvent(savedItem, removalReason);

        kafkaTemplate.send(KafkaProducerConfig.TOPIC_SAVED_ITEM_REMOVED,
                savedItem.getUserId().toString(), event);

        log.info("Published saved item removed event: {}", event);
    }
}