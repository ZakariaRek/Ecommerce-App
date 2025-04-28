package com.Ecommerce.Loyalty_Service.Listeners;

import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Services.Kafka.TransactionKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for PointTransaction documents to automatically publish events to Kafka
 * when points transactions are recorded
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointTransactionMongoListener extends AbstractMongoEventListener<PointTransaction> {

    private final TransactionKafkaService kafkaService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<PointTransaction> event) {
        PointTransaction transaction = event.getSource();
        String key = getEntityKey(transaction);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update (unusual for transactions as they're typically immutable)
                handleTransactionUpdate(transaction, oldState);
            } else {
                // This is a new transaction
                handleTransactionCreation(transaction);
            }
        } catch (Exception e) {
            log.error("Error in point transaction MongoDB listener after save", e);
        }
    }

    /**
     * Handle the creation of a new point transaction
     */
    private void handleTransactionCreation(PointTransaction transaction) {
        kafkaService.publishTransactionRecorded(transaction);
        log.debug("MongoDB listener triggered for point transaction creation: {}", transaction.getId());
    }

    /**
     * Handle updates to an existing point transaction (unusual)
     */
    private void handleTransactionUpdate(PointTransaction transaction, EntityState oldState) {
        // Only publish if something significant changed
        if (oldState.points != transaction.getPoints() ||
                oldState.balance != transaction.getBalance() ||
                oldState.type != transaction.getType()) {

            kafkaService.publishTransactionRecorded(transaction);
            log.debug("MongoDB listener triggered for point transaction update: {}", transaction.getId());
        }
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(PointTransaction transaction) {
        String key = getEntityKey(transaction);
        entityStateMap.put(key, new EntityState(
                transaction.getPoints(),
                transaction.getBalance(),
                transaction.getType()
        ));
        log.debug("Stored state before point transaction save: {}", transaction.getId());
    }

    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(PointTransaction transaction) {
        return "PointTransaction:" + transaction.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final int points;
        private final int balance;
        private final TransactionType type;

        public EntityState(int points, int balance, TransactionType type) {
            this.points = points;
            this.balance = balance;
            this.type = type;
        }
    }
}