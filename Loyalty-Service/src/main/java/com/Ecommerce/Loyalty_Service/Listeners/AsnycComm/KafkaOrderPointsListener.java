package com.Ecommerce.Loyalty_Service.Listeners.AsnycComm;

import com.Ecommerce.Loyalty_Service.Config.KafkaConfig;

import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Events.ExternalEvents;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import com.Ecommerce.Loyalty_Service.Services.PointTransactionService;
import com.Ecommerce.Loyalty_Service.Services.UserSpendingTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service to listen for external Kafka events that impact the loyalty system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderPointsListener {

    private final PointTransactionService transactionService;
    private final CRMRepository crmRepository;
    private final UserSpendingTrackingService spendingTrackingService;

    /**
     * Listen for completed orders to award loyalty points and check CRM registration
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_ORDER_COMPLETED,
            containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCompletedEvent(@Payload ExternalEvents.OrderCompletedEvent event) {
        log.info("🎯 LOYALTY SERVICE: Received order completed event for order: {}, user: {}, amount: ${}",
                event.getOrderId(), event.getUserId(), event.getOrderTotal());

        try {
            // First, check if user should be registered to CRM based on spending
            spendingTrackingService.processOrderCompletion(event.getUserId(), event.getOrderTotal());

            // Now process loyalty points (only if user has CRM record)
            if (crmRepository.findByUserId(event.getUserId()).isPresent()) {
                processLoyaltyPoints(event);
                log.info("✅ LOYALTY SERVICE: Processed loyalty points for user {}", event.getUserId());
            } else {
                log.info("⏳ LOYALTY SERVICE: User {} not yet registered in CRM, points processing skipped",
                        event.getUserId());
            }

        } catch (Exception e) {
            log.error("❌ LOYALTY SERVICE: Error processing order completed event", e);
            throw e; // Re-throw to trigger retry according to error handling policy
        }
    }

    /**
     * Process loyalty points for completed orders
     */
    /**
     * Process loyalty points with idempotency to prevent duplicate processing
     */
    private void processLoyaltyPoints(ExternalEvents.OrderCompletedEvent event) {
        try {
            // Create idempotency key based on order ID to prevent duplicate processing
            String idempotencyKey = "order-" + event.getOrderId().toString();

            // Calculate points based on order total
            int pointsToAward = calculatePointsForOrder(event);

            // Award bonus points for first order if applicable
            if (event.isFirstOrder()) {
                pointsToAward += 100; // Bonus 100 points for first order
                log.info("🎁 LOYALTY SERVICE: Added 100 bonus points for first order");
            }

            // Award points based on payment method
            if ("CREDIT_CARD".equals(event.getPaymentMethod())) {
                pointsToAward += 10; // Bonus for using credit card
                log.info("💳 LOYALTY SERVICE: Added 10 bonus points for credit card payment");
            }

            // Use the new idempotency-enabled method
            PointTransaction transaction = transactionService.recordTransactionWithIdempotency(
                    event.getUserId(),
                    TransactionType.EARN,
                    pointsToAward,
                    "Order: " + event.getOrderId(),
                    idempotencyKey
            );

            log.info("⭐ LOYALTY SERVICE: Awarded {} points to user {} for order {} (Transaction ID: {})",
                    pointsToAward, event.getUserId(), event.getOrderId(), transaction.getId());

        } catch (Exception e) {
            log.error("❌ LOYALTY SERVICE: Error processing loyalty points for order {}",
                    event.getOrderId(), e);
            throw e; // Re-throw to prevent acknowledgment and trigger retry
        }
    }

    /**
     * Listen for user registration events to create loyalty profiles
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_USER_REGISTERED,
            containerFactory = "kafkaListenerContainerFactory")
    public void handleUserRegisteredEvent(@Payload ExternalEvents.UserRegisteredEvent event, Acknowledgment ack) {
        log.info("👤 LOYALTY SERVICE: Received user registered event for user: {}", event.getUserId());

        try {
            // Check if user already has a CRM profile
            if (crmRepository.findByUserId(event.getUserId()).isPresent()) {
                log.info("👤 LOYALTY SERVICE: User {} already has a loyalty profile", event.getUserId());
                ack.acknowledge();
                return;
            }

            // For new user registration, we don't automatically create CRM record
            // They need to reach $150 in orders first
            log.info("👤 LOYALTY SERVICE: User {} registered. CRM record will be created after $150 in orders",
                    event.getUserId());

            // We could track this user for future reference if needed
            // For now, just acknowledge the event
            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ LOYALTY SERVICE: Error processing user registered event", e);
            // Don't acknowledge - will be redelivered
        }
    }

    /**
     * Listen for product review events to award points for reviews
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PRODUCT_REVIEWED,
            containerFactory = "kafkaListenerContainerFactory")
    public void handleProductReviewedEvent(@Payload ExternalEvents.ProductReviewedEvent event, Acknowledgment ack) {
        log.info("📝 LOYALTY SERVICE: Received product reviewed event from user: {}", event.getUserId());

        try {
            // Only award points if user has CRM record and review is for verified purchase
            if (crmRepository.findByUserId(event.getUserId()).isPresent() && event.isVerifiedPurchase()) {
                transactionService.recordTransaction(
                        event.getUserId(),
                        TransactionType.EARN,
                        10, // Points for review
                        "Review: " + event.getProductId()
                );

                log.info("⭐ LOYALTY SERVICE: Awarded 10 points to user {} for reviewing product {}",
                        event.getUserId(), event.getProductId());
            } else {
                log.debug("👤 LOYALTY SERVICE: User {} not in CRM or review not verified, skipping points",
                        event.getUserId());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ LOYALTY SERVICE: Error processing product reviewed event", e);
            // Don't acknowledge - will be redelivered
        }
    }

    /**
     * Listen for cart abandoned events to potentially send targeted offers
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_CART_ABANDONED,
            containerFactory = "kafkaListenerContainerFactory")
    public void handleCartAbandonedEvent(@Payload ExternalEvents.CartAbandonedEvent event, Acknowledgment ack) {
        log.info("🛒 LOYALTY SERVICE: Received cart abandoned event for user: {}", event.getUserId());

        try {
            // Check if user is close to Bronze tier threshold
            BigDecimal remaining = spendingTrackingService.getRemainingForBronze(event.getUserId());

            if (remaining.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(new BigDecimal("50")) <= 0) {
                log.info("💡 LOYALTY SERVICE: User {} is close to Bronze tier (${} remaining), " +
                        "could send targeted promotion", event.getUserId(), remaining);
                // Here you could trigger email marketing or push notifications
            }

            log.info("🛒 LOYALTY SERVICE: User {} abandoned cart with value {} containing {} items",
                    event.getUserId(), event.getCartTotal(), event.getItemCount());

            ack.acknowledge();
        } catch (Exception e) {
            log.error("❌ LOYALTY SERVICE: Error processing cart abandoned event", e);
            // Don't acknowledge - will be redelivered
        }
    }

    /**
     * Calculate loyalty points for an order
     * This is a simplified implementation:
     * - Base calculation: 1 point per $1 spent
     * - Minimum $5 purchase for any points
     * - Random bonus points for promotional effect
     */
    private int calculatePointsForOrder(ExternalEvents.OrderCompletedEvent event) {
        BigDecimal orderTotal = event.getOrderTotal();

        // Minimum purchase amount check
        if (orderTotal.compareTo(new BigDecimal("5.00")) < 0) {
            return 0;
        }

        // Basic calculation: 1 point per $1
        int basePoints = orderTotal.intValue();

        // Add random bonus points (5% chance of 2x multiplier)
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            basePoints *= 2;
            log.info("🍀 LOYALTY SERVICE: Lucky 2x point multiplier applied for order {}", event.getOrderId());
        }

        return basePoints;
    }
}