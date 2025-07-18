package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track user spending and automatically register users to CRM
 * when they reach spending thresholds
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSpendingTrackingService {

    private final CRMRepository crmRepository;

    // In-memory cache to track user spending (in production, use Redis or database)
    private final Map<UUID, BigDecimal> userSpendingCache = new ConcurrentHashMap<>();

    // Spending threshold for Bronze tier registration
    private static final BigDecimal BRONZE_TIER_THRESHOLD = new BigDecimal("150.00");

    /**
     * Process an order completion and check if user should be registered to CRM
     */
    @Transactional
    public void processOrderCompletion(UUID userId, BigDecimal orderAmount) {
        log.info("💰 LOYALTY SERVICE: Processing order completion for user {} with amount {}",
                userId, orderAmount);

        // Check if user already has CRM record
        if (crmRepository.findByUserId(userId).isPresent()) {
            log.debug("💰 LOYALTY SERVICE: User {} already has CRM record, skipping registration check",
                    userId);
            return;
        }

        // Update user's total spending
        BigDecimal currentSpending = userSpendingCache.getOrDefault(userId, BigDecimal.ZERO);
        BigDecimal newTotalSpending = currentSpending.add(orderAmount);
        userSpendingCache.put(userId, newTotalSpending);

        log.info("💰 LOYALTY SERVICE: User {} total spending updated: {} -> {}",
                userId, currentSpending, newTotalSpending);

        // Check if user has reached Bronze tier threshold
        if (newTotalSpending.compareTo(BRONZE_TIER_THRESHOLD) >= 0) {
            registerUserToCRM(userId, newTotalSpending);
        } else {
            BigDecimal remaining = BRONZE_TIER_THRESHOLD.subtract(newTotalSpending);
            log.info("💰 LOYALTY SERVICE: User {} needs ${} more to reach Bronze tier",
                    userId, remaining);
        }
    }

    /**
     * Register user to CRM with Bronze tier
     */
    @Transactional
    public void registerUserToCRM(UUID userId, BigDecimal totalSpent) {
        try {
            log.info("🎉 LOYALTY SERVICE: Registering user {} to CRM with Bronze tier (total spent: ${})",
                    userId, totalSpent);

            CRM crm = new CRM();
            crm.setId(UUID.randomUUID());
            crm.setUserId(userId);
            crm.setTotalPoints(0); // Start with 0 points, will be awarded through normal flow
            crm.setMembershipLevel(MembershipTier.BRONZE);
            crm.setJoinDate(LocalDateTime.now());
            crm.setLastActivity(LocalDateTime.now());

            CRM savedCrm = crmRepository.save(crm);

            // Clear from spending cache since user is now registered
            userSpendingCache.remove(userId);

            log.info("✅ LOYALTY SERVICE: Successfully registered user {} to CRM with Bronze tier. CRM ID: {}",
                    userId, savedCrm.getId());

            // You could also publish a Kafka event here for other services
            // publishUserRegisteredToCRM(userId, MembershipTier.BRONZE, totalSpent);

        } catch (Exception e) {
            log.error("❌ LOYALTY SERVICE: Failed to register user {} to CRM", userId, e);
        }
    }

    /**
     * Get current spending for a user (for debugging/admin purposes)
     */
    public BigDecimal getUserCurrentSpending(UUID userId) {
        return userSpendingCache.getOrDefault(userId, BigDecimal.ZERO);
    }

    /**
     * Get remaining amount needed for Bronze tier
     */
    public BigDecimal getRemainingForBronze(UUID userId) {
        BigDecimal currentSpending = userSpendingCache.getOrDefault(userId, BigDecimal.ZERO);
        BigDecimal remaining = BRONZE_TIER_THRESHOLD.subtract(currentSpending);
        return remaining.max(BigDecimal.ZERO);
    }

    /**
     * Check if user is eligible for CRM registration based on spending
     */
    public boolean isEligibleForCRMRegistration(UUID userId) {
        // User is eligible if they don't have CRM record and have spent >= $150
        if (crmRepository.findByUserId(userId).isPresent()) {
            return false;
        }

        BigDecimal currentSpending = userSpendingCache.getOrDefault(userId, BigDecimal.ZERO);
        return currentSpending.compareTo(BRONZE_TIER_THRESHOLD) >= 0;
    }

    /**
     * Manual registration trigger (for admin use or edge cases)
     */
    @Transactional
    public boolean manuallyRegisterUserToCRM(UUID userId) {
        if (crmRepository.findByUserId(userId).isPresent()) {
            log.warn("💰 LOYALTY SERVICE: User {} already has CRM record", userId);
            return false;
        }

        BigDecimal currentSpending = userSpendingCache.getOrDefault(userId, BigDecimal.ZERO);
        registerUserToCRM(userId, currentSpending);
        return true;
    }

    /**
     * Reset user spending cache (for testing or maintenance)
     */
    public void resetUserSpendingCache() {
        userSpendingCache.clear();
        log.info("💰 LOYALTY SERVICE: User spending cache reset");
    }

    /**
     * Get spending threshold for Bronze tier
     */
    public BigDecimal getBronzeTierThreshold() {
        return BRONZE_TIER_THRESHOLD;
    }
}