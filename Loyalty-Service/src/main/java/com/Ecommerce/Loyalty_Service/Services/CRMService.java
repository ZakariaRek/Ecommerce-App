package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CRMService {
    @Autowired
    private CRMRepository crmRepository;

    public CRM getByUserId(UUID userId) {
        return crmRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found in loyalty system"));
    }

    @Transactional
    public void earnPoints(UUID userId, int points, String source) {
        CRM crm = getByUserId(userId);
        crm.setTotalPoints(crm.getTotalPoints() + points);
        crm.setLastActivity(LocalDateTime.now());
        upgradeMembership(crm);
        crmRepository.save(crm);
    }

    @Transactional
    public void redeemPoints(UUID userId, int points) {
        CRM crm = getByUserId(userId);
        if (crm.getTotalPoints() < points) {
            throw new RuntimeException("Insufficient points balance");
        }
        crm.setTotalPoints(crm.getTotalPoints() - points);
        crm.setLastActivity(LocalDateTime.now());
        upgradeMembership(crm);
        crmRepository.save(crm);
    }

    private void upgradeMembership(CRM crm) {
        int points = crm.getTotalPoints();

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
}

