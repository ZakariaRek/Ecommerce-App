package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.config.KafkaConfig;
import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Events.ExternalEvents;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import com.Ecommerce.Loyalty_Service.Services.CRMService;
import com.Ecommerce.Loyalty_Service.Services.PointTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service to listen for external Kafka events that impact the loyalty system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerService {

    private final CRMService crmService;
    private final PointTransactionService transactionService;
    private final CRMRepository crmRepository;

    /**
     * Listen for completed orders to award loyalty points
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_ORDER_COMPLETED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderCompletedEvent(@Payload ExternalEvents.OrderCompletedEvent event, Acknowledgment ack) {
        log.info("Received order completed event for order: {}", event.getOrderId());

        try {
            // Calculate points based on order total
            int pointsToAward = calculatePointsForOrder(event);

            // Award bonus points for first order if applicable
            if (event.isFirstOrder()) {
                pointsToAward += 100; // Bonus 100 points for first order
            }

            // Award points based on payment method (example of rule-based points)
            if ("CREDIT_CARD".equals(event.getPaymentMethod())) {
                pointsToAward += 10; // Bonus for using credit card
            }

            // Record the transaction and update user's points
            transactionService.recordTransaction(
                    event.getUserId(),
                    TransactionType.EARN,
                    pointsToAward,
                    "Order: " + event.getOrderId()
            );

            log.info("Awarded {} points to user {} for order {}",
                    pointsToAward, event.getUserId(), event.getOrderId());

            // Acknowledge the message after successful processing
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order completed event", e);
            // Don't acknowledge - will be redelivered according to retry policy
            // In a production system, you might want to implement a dead letter queue
        }
    }

    /**
     * Listen for user registration events to create loyalty profiles
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_USER_REGISTERED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleUserRegisteredEvent(@Payload ExternalEvents.UserRegisteredEvent event, Acknowledgment ack) {
        log.info("Received user registered event for user: {}", event.getUserId());

        try {
            // Check if user already has a CRM profile
            if (crmRepository.findByUserId(event.getUserId()).isPresent()) {
                log.info("User {} already has a loyalty profile", event.getUserId());
                ack.acknowledge();
                return;
            }

            // Create new CRM profile
            CRM crm = new CRM();
            crm.setId(UUID.randomUUID());
            crm.setUserId(event.getUserId());
            crm.setTotalPoints(0);
            crm.setMembershipLevel(MembershipTier.BRONZE);
            crm.setJoinDate(event.getRegistrationDate());
            crm.setLastActivity(LocalDateTime.now());

            crmRepository.save(crm);

            // Award welcome points
            transactionService.recordTransaction(
                    event.getUserId(),
                    TransactionType.EARN,
                    50, // Welcome bonus points
                    "Welcome Bonus"
            );

            // Award referral bonus if applicable
            if (event.getReferredBy() != null && !event.getReferredBy().isEmpty()) {
                try {
                    UUID referrerId = UUID.fromString(event.getReferredBy());
                    // Award points to the referrer
                    transactionService.recordTransaction(
                            referrerId,
                            TransactionType.EARN,
                            100, // Referral bonus points
                            "Referral Bonus: " + event.getUserId()
                    );
                    log.info("Awarded referral bonus to user {}", referrerId);
                } catch (Exception e) {
                    log.error("Error processing referral bonus", e);
                }
            }

            log.info("Created loyalty profile for user {}", event.getUserId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user registered event", e);
            // Don't acknowledge - will be redelivered
        }
    }

    /**
     * Listen for product review events to award points for reviews
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_PRODUCT_REVIEWED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleProductReviewedEvent(@Payload ExternalEvents.ProductReviewedEvent event, Acknowledgment ack) {
        log.info("Received product reviewed event from user: {}", event.getUserId());

        try {
            // Only award points for verified purchases
            if (event.isVerifiedPurchase()) {
                // Award points based on the length and quality of the review
                // For this example, we'll simply award 10 points for any review
                transactionService.recordTransaction(
                        event.getUserId(),
                        TransactionType.EARN,
                        10, // Points for review
                        "Review: " + event.getProductId()
                );

                log.info("Awarded 10 points to user {} for reviewing product {}",
                        event.getUserId(), event.getProductId());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing product reviewed event", e);
            // Don't acknowledge - will be redelivered
        }
    }

    /**
     * Listen for cart abandoned events to potentially send targeted offers
     */
    @KafkaListener(
            topics = KafkaConfig.TOPIC_CART_ABANDONED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleCartAbandonedEvent(@Payload ExternalEvents.CartAbandonedEvent event, Acknowledgment ack) {
        log.info("Received cart abandoned event for user: {}", event.getUserId());

        try {
            // In a real implementation, you might trigger an email with a special coupon
            // or add the user to a targeted campaign
            // For this example, we'll just log the event
            log.info("User {} abandoned cart with value {} containing {} items",
                    event.getUserId(), event.getCartTotal(), event.getItemCount());

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing cart abandoned event", e);
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
            log.info("Lucky 2x point multiplier applied for order {}", event.getOrderId());
        }

        return basePoints;
    }
}