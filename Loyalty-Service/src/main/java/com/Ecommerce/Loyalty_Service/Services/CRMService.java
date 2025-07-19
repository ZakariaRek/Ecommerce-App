package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CRMKafkaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CRMService {
    private final CRMRepository crmRepository;
    private final CRMKafkaService kafkaService;

    public CRM getByUserId(UUID userId) {
        return crmRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found in loyalty system"));
    }

    @Transactional
    public void earnPoints(UUID userId, int points, String source) {
        CRM crm = getByUserId(userId);

        // Store previous state for comparison
        MembershipTier previousTier = crm.getMembershipLevel();
        int previousPoints = crm.getTotalPoints();


        // Update points and last activity
        crm.setTotalPoints(previousPoints + points);
        crm.setLastActivity(LocalDateTime.now());

        // Check if tier should be upgraded
        upgradeMembership(crm);

        // Save the updated CRM record
        CRM updatedCrm = crmRepository.save(crm);

        // Direct Kafka event - for cases when the MongoDB listener might not trigger
        // or when we need to include additional context (like source ID)
        kafkaService.publishPointsEarned(
                userId,
                points,
                updatedCrm.getTotalPoints(),
                source,
                null, // No specific source ID
                updatedCrm.getMembershipLevel()
        );

        // If membership tier changed, send that event too
        if (previousTier != updatedCrm.getMembershipLevel()) {
            kafkaService.publishMembershipTierChanged(
                    userId,
                    previousTier,
                    updatedCrm.getMembershipLevel(),
                    updatedCrm.getTotalPoints(),
                    "POINTS_INCREASE"
            );

            log.info("User {} upgraded from {} to {}",
                    userId, previousTier, updatedCrm.getMembershipLevel());
        }
    }

    @Transactional
    public void redeemPoints(UUID userId, int points) {
        CRM crm = getByUserId(userId);
        if (crm.getTotalPoints() < points) {
            throw new RuntimeException("Insufficient points balance");
        }

        // Store previous state for comparison
        MembershipTier previousTier = crm.getMembershipLevel();
        int previousPoints = crm.getTotalPoints();



        // Update points and last activity
        crm.setTotalPoints(previousPoints - points);
        crm.setLastActivity(LocalDateTime.now());

        // Check if tier should be updated
        upgradeMembership(crm);

        // Save the updated CRM record
        CRM updatedCrm = crmRepository.save(crm);

        // Direct Kafka event
        kafkaService.publishPointsRedeemed(
                userId,
                points,
                updatedCrm.getTotalPoints(),
                "POINTS_REDEMPTION",
                null, // No specific purpose ID
                updatedCrm.getMembershipLevel()
        );

        // If membership tier changed, send that event too
        if (previousTier != updatedCrm.getMembershipLevel()) {
            kafkaService.publishMembershipTierChanged(
                    userId,
                    previousTier,
                    updatedCrm.getMembershipLevel(),
                    updatedCrm.getTotalPoints(),
                    "POINTS_DECREASE"
            );

            log.info("User {} downgraded from {} to {}",
                    userId, previousTier, updatedCrm.getMembershipLevel());
        }
    }

    private void upgradeMembership(CRM crm) {
        int points = crm.getTotalPoints();
        MembershipTier previousTier = crm.getMembershipLevel();

        if (points >= 10000) {
            crm.setMembershipLevel(MembershipTier.DIAMOND);
        } else if (points >= 5000) {
            crm.setMembershipLevel(MembershipTier.PLATINUM);
        } else if (points >= 2000) {
            crm.setMembershipLevel(MembershipTier.GOLD);
        } else if (points >= 500) {
            crm.setMembershipLevel(MembershipTier.SILVER);
        } else {
            crm.setMembershipLevel(MembershipTier.BRONZE);
        }
    }

    public double calculateLoyaltyScore(UUID userId) {
        CRM crm = getByUserId(userId);

        // Calculate days between dates without using ChronoUnit
        LocalDateTime now = LocalDateTime.now();
        long membershipDays = Duration.between(crm.getJoinDate(), now).toDays();

        // Calculate activity score
        long daysSinceLastActivity = Duration.between(crm.getLastActivity(), now).toDays();
        double activityScore = daysSinceLastActivity <= 30 ? 1.0 : 0.5;

        return (crm.getTotalPoints() / 100.0) * (Math.log10(membershipDays + 1)) * activityScore;
    }

    /**
     * Adjust points for a user (admin function)
     */
    @Transactional
    public void adjustPoints(UUID userId, int pointsAdjustment, String reason, String adjustedBy) {
        CRM crm = getByUserId(userId);

        // Store previous state for comparison
        MembershipTier previousTier = crm.getMembershipLevel();
        int previousPoints = crm.getTotalPoints();


        // Update points
        crm.setTotalPoints(previousPoints + pointsAdjustment);
        crm.setLastActivity(LocalDateTime.now());

        // Check if tier should be updated
        upgradeMembership(crm);

        // Save the updated CRM record
        CRM updatedCrm = crmRepository.save(crm);

        // Direct Kafka event
        kafkaService.publishPointsAdjusted(
                userId,
                pointsAdjustment,
                updatedCrm.getTotalPoints(),
                reason,
                adjustedBy,
                updatedCrm.getMembershipLevel()
        );

        // If membership tier changed, send that event too
        if (previousTier != updatedCrm.getMembershipLevel()) {
            kafkaService.publishMembershipTierChanged(
                    userId,
                    previousTier,
                    updatedCrm.getMembershipLevel(),
                    updatedCrm.getTotalPoints(),
                    "ADMIN_ADJUSTMENT"
            );

            log.info("User {} membership changed from {} to {} due to admin adjustment",
                    userId, previousTier, updatedCrm.getMembershipLevel());
        }
    }

    public List<CRM> getAllUsers() {
        return crmRepository.findAll();
    }
}