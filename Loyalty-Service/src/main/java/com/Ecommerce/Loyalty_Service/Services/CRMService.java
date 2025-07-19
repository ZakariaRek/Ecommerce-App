package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Config.TierThresholdConfig;
import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Payload.Request.Transaction.TierProgressInfo;
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
    private final TierThresholdConfig tierConfig;

    public CRM getByUserId(UUID userId) {
        return crmRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found in loyalty system"));
    }

    @Transactional
    public void earnPoints(UUID userId, int points, String source) {
        CRM crm = getByUserId(userId);

        MembershipTier previousTier = crm.getMembershipLevel();
        int previousPoints = crm.getTotalPoints();

        crm.setTotalPoints(previousPoints + points);
        crm.setLastActivity(LocalDateTime.now());

        upgradeMembership(crm);

        CRM updatedCrm = crmRepository.save(crm);

        kafkaService.publishPointsEarned(
                userId,
                points,
                updatedCrm.getTotalPoints(),
                source,
                null,
                updatedCrm.getMembershipLevel()
        );

        if (previousTier != updatedCrm.getMembershipLevel()) {
            kafkaService.publishMembershipTierChanged(
                    userId,
                    previousTier,
                    updatedCrm.getMembershipLevel(),
                    updatedCrm.getTotalPoints(),
                    "POINTS_INCREASE"
            );

            log.info("🎉 User {} upgraded from {} to {}",
                    userId, previousTier, updatedCrm.getMembershipLevel());
        }
    }

    @Transactional
    public void redeemPoints(UUID userId, int points) {
        CRM crm = getByUserId(userId);
        if (crm.getTotalPoints() < points) {
            throw new RuntimeException("Insufficient points balance");
        }

        MembershipTier previousTier = crm.getMembershipLevel();
        int previousPoints = crm.getTotalPoints();

        crm.setTotalPoints(previousPoints - points);
        crm.setLastActivity(LocalDateTime.now());

        upgradeMembership(crm);

        CRM updatedCrm = crmRepository.save(crm);

        kafkaService.publishPointsRedeemed(
                userId,
                points,
                updatedCrm.getTotalPoints(),
                "POINTS_REDEMPTION",
                null,
                updatedCrm.getMembershipLevel()
        );

        if (previousTier != updatedCrm.getMembershipLevel()) {
            kafkaService.publishMembershipTierChanged(
                    userId,
                    previousTier,
                    updatedCrm.getMembershipLevel(),
                    updatedCrm.getTotalPoints(),
                    "POINTS_DECREASE"
            );

            log.info("⬇️ User {} tier changed from {} to {} due to point redemption",
                    userId, previousTier, updatedCrm.getMembershipLevel());
        }
    }

    /**
     * Enhanced tier upgrade logic with configurable thresholds
     */
    private void upgradeMembership(CRM crm) {
        int points = crm.getTotalPoints();
        MembershipTier previousTier = crm.getMembershipLevel();

        if (points >= tierConfig.getDiamondThreshold()) {
            crm.setMembershipLevel(MembershipTier.DIAMOND);
        } else if (points >= tierConfig.getPlatinumThreshold()) {
            crm.setMembershipLevel(MembershipTier.PLATINUM);
        } else if (points >= tierConfig.getGoldThreshold()) {
            crm.setMembershipLevel(MembershipTier.GOLD);
        } else if (points >= tierConfig.getSilverThreshold()) {
            crm.setMembershipLevel(MembershipTier.SILVER);
        } else {
            crm.setMembershipLevel(MembershipTier.BRONZE);
        }
    }

    /**
     * Get points needed for next tier
     */
    public int getPointsNeededForNextTier(UUID userId) {
        CRM crm = getByUserId(userId);
        int currentPoints = crm.getTotalPoints();
        MembershipTier currentTier = crm.getMembershipLevel();

        return switch (currentTier) {
            case BRONZE -> Math.max(0, tierConfig.getSilverThreshold() - currentPoints);
            case SILVER -> Math.max(0, tierConfig.getGoldThreshold() - currentPoints);
            case GOLD -> Math.max(0, tierConfig.getPlatinumThreshold() - currentPoints);
            case PLATINUM -> Math.max(0, tierConfig.getDiamondThreshold() - currentPoints);
            case DIAMOND -> 0; // Already at highest tier
        };
    }

    /**
     * Get next tier information
     */
    public MembershipTier getNextTier(UUID userId) {
        CRM crm = getByUserId(userId);
        MembershipTier currentTier = crm.getMembershipLevel();

        return switch (currentTier) {
            case BRONZE -> MembershipTier.SILVER;
            case SILVER -> MembershipTier.GOLD;
            case GOLD -> MembershipTier.PLATINUM;
            case PLATINUM -> MembershipTier.DIAMOND;
            case DIAMOND -> MembershipTier.DIAMOND; // Already at highest
        };
    }

    /**
     * Get tier progress information
     */
    public TierProgressInfo getTierProgress(UUID userId) {
        CRM crm = getByUserId(userId);
        int currentPoints = crm.getTotalPoints();
        MembershipTier currentTier = crm.getMembershipLevel();

        int tierStartPoints = switch (currentTier) {
            case BRONZE -> tierConfig.getBronzeThreshold();
            case SILVER -> tierConfig.getSilverThreshold();
            case GOLD -> tierConfig.getGoldThreshold();
            case PLATINUM -> tierConfig.getPlatinumThreshold();
            case DIAMOND -> tierConfig.getDiamondThreshold();
        };

        int nextTierPoints = switch (currentTier) {
            case BRONZE -> tierConfig.getSilverThreshold();
            case SILVER -> tierConfig.getGoldThreshold();
            case GOLD -> tierConfig.getPlatinumThreshold();
            case PLATINUM -> tierConfig.getDiamondThreshold();
            case DIAMOND -> tierConfig.getDiamondThreshold(); // Max tier
        };

        int pointsInCurrentTier = currentPoints - tierStartPoints;
        int pointsNeededForTier = nextTierPoints - tierStartPoints;
        double progressPercentage = currentTier == MembershipTier.DIAMOND ? 100.0 :
                (double) pointsInCurrentTier / pointsNeededForTier * 100.0;

        return TierProgressInfo.builder()
                .currentTier(currentTier)
                .currentPoints(currentPoints)
                .pointsInCurrentTier(pointsInCurrentTier)
                .pointsNeededForNextTier(getPointsNeededForNextTier(userId))
                .nextTier(getNextTier(userId))
                .progressPercentage(progressPercentage)
                .tierStartPoints(tierStartPoints)
                .nextTierPoints(nextTierPoints)
                .build();
    }

    public double calculateLoyaltyScore(UUID userId) {
        CRM crm = getByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        long membershipDays = Duration.between(crm.getJoinDate(), now).toDays();
        long daysSinceLastActivity = Duration.between(crm.getLastActivity(), now).toDays();
        double activityScore = daysSinceLastActivity <= 30 ? 1.0 : 0.5;

        return (crm.getTotalPoints() / 100.0) * (Math.log10(membershipDays + 1)) * activityScore;
    }

    @Transactional
    public void adjustPoints(UUID userId, int pointsAdjustment, String reason, String adjustedBy) {
        CRM crm = getByUserId(userId);
        MembershipTier previousTier = crm.getMembershipLevel();
        int previousPoints = crm.getTotalPoints();

        crm.setTotalPoints(previousPoints + pointsAdjustment);
        crm.setLastActivity(LocalDateTime.now());

        upgradeMembership(crm);

        CRM updatedCrm = crmRepository.save(crm);

        kafkaService.publishPointsAdjusted(
                userId,
                pointsAdjustment,
                updatedCrm.getTotalPoints(),
                reason,
                adjustedBy,
                updatedCrm.getMembershipLevel()
        );

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