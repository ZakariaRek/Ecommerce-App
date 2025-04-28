package com.Ecommerce.Loyalty_Service.Listeners;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CouponKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Event Listener for Coupon documents to automatically publish events to Kafka
 * when coupons are generated, redeemed, or expired
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponMongoListener extends AbstractMongoEventListener<Coupon> {

    private final CouponKafkaService kafkaService;

    // Store pre-change state for events
    private static final Map<String, EntityState> entityStateMap = new ConcurrentHashMap<>();

    /**
     * Called after a document is saved (created or updated)
     */
    @Override
    public void onAfterSave(AfterSaveEvent<Coupon> event) {
        Coupon coupon = event.getSource();
        String key = getEntityKey(coupon);

        try {
            // Check if we have previous state (update case)
            EntityState oldState = entityStateMap.remove(key);

            if (oldState != null) {
                // This is an update
                handleCouponUpdate(coupon, oldState);
            } else {
                // This is a new coupon
                handleCouponCreation(coupon);
            }
        } catch (Exception e) {
            log.error("Error in coupon MongoDB listener after save", e);
        }
    }

    /**
     * Called before a document is deleted
     */
    @Override
    public void onBeforeDelete(BeforeDeleteEvent<Coupon> event) {
        try {
            // We don't have direct access to the document here, just the id
            Object id = event.getSource().get("_id");
            log.debug("Preparing to delete coupon with id: {}", id);

            // We can't access the actual Coupon here
            // The service layer should handle publishing removal events
        } catch (Exception e) {
            log.error("Error in coupon MongoDB listener before delete", e);
        }
    }

    /**
     * Called after a document is deleted
     */
    @Override
    public void onAfterDelete(AfterDeleteEvent<Coupon> event) {
        try {
            // Similar to onBeforeDelete, we only have access to the document id
            Object id = event.getSource().get("_id");
            log.debug("Coupon with id: {} was deleted", id);
        } catch (Exception e) {
            log.error("Error in coupon MongoDB listener after delete", e);
        }
    }

    /**
     * Handle the creation of a new coupon
     */
    private void handleCouponCreation(Coupon coupon) {
        kafkaService.publishCouponGenerated(coupon, "SYSTEM_GENERATED");
        log.debug("MongoDB listener triggered for coupon creation: {}", coupon.getId());
    }

    /**
     * Handle updates to an existing coupon
     */
    private void handleCouponUpdate(Coupon coupon, EntityState oldState) {
        // Check what changed
        if (!oldState.used && coupon.isUsed()) {
            // Coupon was used/redeemed
            // In a real implementation, you would have the order data
            // For now, using placeholder data
            kafkaService.publishCouponRedeemed(
                    coupon,
                    oldState.minPurchaseAmount, // Using minPurchaseAmount as original amount placeholder
                    coupon.getDiscountValue(),
                    oldState.minPurchaseAmount.subtract(coupon.getDiscountValue()),
                    null // Order ID would typically be available
            );
            log.debug("Coupon {} was redeemed", coupon.getCode());
        }

        // Check if a coupon expired since last save
        if (!oldState.isExpired && isExpired(coupon)) {
            kafkaService.publishCouponExpired(coupon);
            log.debug("Coupon {} expired", coupon.getCode());
        }

        log.debug("MongoDB listener triggered for coupon update: {}", coupon.getId());
    }

    /**
     * Check if a coupon is expired
     */
    private boolean isExpired(Coupon coupon) {
        return coupon.getExpirationDate().isBefore(LocalDateTime.now());
    }

    /**
     * Store state before save for later comparison in afterSave
     * This should be called by the service layer before saving changes
     */
    public void storeStateBeforeSave(Coupon coupon) {
        String key = getEntityKey(coupon);
        boolean isExpired = coupon.getExpirationDate().isBefore(LocalDateTime.now());
        entityStateMap.put(key, new EntityState(
                coupon.isUsed(),
                isExpired,
                coupon.getMinPurchaseAmount()
        ));
        log.debug("Stored state before coupon save: {}", coupon.getId());
    }

    /**
     * Generate a unique key for the entity
     */
    private String getEntityKey(Coupon coupon) {
        return "Coupon:" + coupon.getId();
    }

    /**
     * Simple class to store entity state
     */
    private static class EntityState {
        private final boolean used;
        private final boolean isExpired;
        private final java.math.BigDecimal minPurchaseAmount;

        public EntityState(boolean used, boolean isExpired, java.math.BigDecimal minPurchaseAmount) {
            this.used = used;
            this.isExpired = isExpired;
            this.minPurchaseAmount = minPurchaseAmount;
        }
    }
}