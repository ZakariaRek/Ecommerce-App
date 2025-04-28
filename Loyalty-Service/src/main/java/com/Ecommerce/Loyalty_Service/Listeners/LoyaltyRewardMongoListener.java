package com.Ecommerce.Loyalty_Service.Listeners;

import com.Ecommerce.Loyalty_Service.Entities.LoyaltyReward;
import com.Ecommerce.Loyalty_Service.Services.Kafka.RewardKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for LoyaltyReward documents to automatically publish events to Kafka
 * when rewards are added or updated
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyRewardMongoListener extends AbstractMongoEventListener<LoyaltyReward> {

    private final RewardKafkaService kafkaService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<LoyaltyReward> event) {
        LoyaltyReward reward = event.getSource();
        String key = getEntityKey(reward);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update
                handleRewardUpdate(reward, oldState);
            } else {
                // This is a new reward
                handleRewardCreation(reward);
            }
        } catch (Exception e) {
            log.error("Error in loyalty reward MongoDB listener after save", e);
        }
    }

    /**
     * Handle the creation of a new loyalty reward
     */
    private void handleRewardCreation(LoyaltyReward reward) {
        kafkaService.publishRewardAdded(reward, "SYSTEM");
        log.debug("MongoDB listener triggered for loyalty reward creation: {}", reward.getId());
    }

    /**
     * Handle updates to an existing loyalty reward
     */
    private void handleRewardUpdate(LoyaltyReward reward, EntityState oldState) {
        // Create a copy of the old state as a LoyaltyReward object
        LoyaltyReward previousReward = new LoyaltyReward();
        previousReward.setId(reward.getId());
        previousReward.setName(oldState.name);
        previousReward.setDescription(oldState.description);
        previousReward.setPointsCost(oldState.pointsCost);
        previousReward.setActive(oldState.active);
        previousReward.setExpiryDays(oldState.expiryDays);

        // Publish update event
        kafkaService.publishRewardUpdated(reward, previousReward, "SYSTEM");
        log.debug("MongoDB listener triggered for loyalty reward update: {}", reward.getId());
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(LoyaltyReward reward) {
        String key = getEntityKey(reward);
        entityStateMap.put(key, new EntityState(
                reward.getName(),
                reward.getDescription(),
                reward.getPointsCost(),
                reward.isActive(),
                reward.getExpiryDays()
        ));
        log.debug("Stored state before loyalty reward save: {}", reward.getId());
    }

    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(LoyaltyReward reward) {
        return "LoyaltyReward:" + reward.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final String name;
        private final String description;
        private final int pointsCost;
        private final boolean active;
        private final int expiryDays;

        public EntityState(String name, String description, int pointsCost, boolean active, int expiryDays) {
            this.name = name;
            this.description = description;
            this.pointsCost = pointsCost;
            this.active = active;
            this.expiryDays = expiryDays;
        }
    }
}