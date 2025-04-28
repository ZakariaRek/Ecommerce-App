package com.Ecommerce.Loyalty_Service.Listeners;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CRMKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for CRM documents to automatically publish events to Kafka
 * when points are earned, redeemed, or membership tiers change
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CRMMongoListener extends AbstractMongoEventListener<CRM> {

    private final CRMKafkaService kafkaService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<CRM> event) {
        CRM crm = event.getSource();
        String key = getEntityKey(crm);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update
                handleCRMUpdate(crm, oldState);
            } else {
                // This is a new CRM profile
                handleCRMCreation(crm);
            }
        } catch (Exception e) {
            log.error("Error in CRM MongoDB listener after save", e);
        }
    }

    /**
     * Handle the creation of a new CRM profile
     */
    private void handleCRMCreation(CRM crm) {
        // No specific event for CRM creation yet
        log.debug("MongoDB listener triggered for CRM creation: {}", crm.getId());
    }

    /**
     * Handle updates to an existing CRM profile
     */
    private void handleCRMUpdate(CRM crm, EntityState oldState) {
        // Check what changed
        int pointsDifference = crm.getTotalPoints() - oldState.totalPoints;

        // Points changed
        if (pointsDifference != 0) {
            handlePointsChange(crm, oldState.totalPoints, pointsDifference);
        }

        // Membership tier changed
        if (oldState.membershipTier != crm.getMembershipLevel()) {
            handleMembershipTierChange(crm, oldState.membershipTier);
        }

        log.debug("MongoDB listener triggered for CRM update: {}", crm.getId());
    }

    /**
     * Handle changes to points balance
     */
    private void handlePointsChange(CRM crm, int oldTotalPoints, int pointsDifference) {
        // Points were added
        if (pointsDifference > 0) {
            kafkaService.publishPointsEarned(
                    crm.getUserId(),
                    pointsDifference,
                    crm.getTotalPoints(),
                    "SYSTEM_UPDATE", // Generic source
                    null, // No specific source ID
                    crm.getMembershipLevel()
            );
            log.debug("Points increased by {} for user {}", pointsDifference, crm.getUserId());
        }
        // Points were deducted
        else if (pointsDifference < 0) {
            kafkaService.publishPointsRedeemed(
                    crm.getUserId(),
                    Math.abs(pointsDifference),
                    crm.getTotalPoints(),
                    "SYSTEM_UPDATE", // Generic purpose
                    null, // No specific purpose ID
                    crm.getMembershipLevel()
            );
            log.debug("Points decreased by {} for user {}", Math.abs(pointsDifference), crm.getUserId());
        }
    }

    /**
     * Handle changes to membership tier
     */
    private void handleMembershipTierChange(CRM crm, MembershipTier oldTier) {
        String reason = crm.getTotalPoints() > 0 ? "POINTS_CHANGE" : "SYSTEM_UPDATE";

        kafkaService.publishMembershipTierChanged(
                crm.getUserId(),
                oldTier,
                crm.getMembershipLevel(),
                crm.getTotalPoints(),
                reason
        );

        log.debug("Membership tier changed from {} to {} for user {}",
                oldTier, crm.getMembershipLevel(), crm.getUserId());
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(CRM crm) {
        String key = getEntityKey(crm);
        entityStateMap.put(key, new EntityState(crm.getTotalPoints(), crm.getMembershipLevel()));
        log.debug("Stored state before CRM save: {}", crm.getId());
    }

    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(CRM crm) {
        return "CRM:" + crm.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final int totalPoints;
        private final MembershipTier membershipTier;

        public EntityState(int totalPoints, MembershipTier membershipTier) {
            this.totalPoints = totalPoints;
            this.membershipTier = membershipTier;
        }
    }
}