package com.Ecommerce.Loyalty_Service.Factory;

import com.Ecommerce.Loyalty_Service.Entities.*;
import com.Ecommerce.Loyalty_Service.Repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.UUID;

/**
 * Factory to populate database with default loyalty system data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltyDefaultDataFactory implements CommandLineRunner {

    private final TierBenefitRepository tierBenefitRepository;
    private final LoyaltyRewardRepository loyaltyRewardRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🏭 Starting Loyalty Service Default Data Factory...");

        createDefaultTierBenefits();
        createDefaultLoyaltyRewards();

        log.info("✅ Loyalty Service Default Data Factory completed successfully!");
    }

    /**
     * Create default tier benefits for each membership level
     */
    private void createDefaultTierBenefits() {
        log.info("🎖️ Creating default tier benefits...");

        // Check if tier benefits already exist
        if (tierBenefitRepository.count() > 0) {
            log.info("⏭️ Tier benefits already exist, skipping creation");
            return;
        }

        // BRONZE TIER BENEFITS
        createTierBenefit(
                MembershipTier.BRONZE,
                BenefitType.BIRTHDAY_BONUS,
                null, // No discount percentage for birthday bonus
                null, // No max discount
                null, // No min order
                "50 bonus points on birthday"
        );

        // SILVER TIER BENEFITS
        createTierBenefit(
                MembershipTier.SILVER,
                BenefitType.DISCOUNT,
                new BigDecimal("3.00"), // 3% discount
                new BigDecimal("25.00"), // Max $25 discount
                new BigDecimal("50.00"), // Min $50 order
                "3% discount on orders over $50"
        );

        createTierBenefit(
                MembershipTier.SILVER,
                BenefitType.BIRTHDAY_BONUS,
                null,
                null,
                null,
                "100 bonus points on birthday"
        );

        // GOLD TIER BENEFITS
        createTierBenefit(
                MembershipTier.GOLD,
                BenefitType.DISCOUNT,
                new BigDecimal("5.00"), // 5% discount
                new BigDecimal("50.00"), // Max $50 discount
                new BigDecimal("25.00"), // Min $25 order
                "5% discount on orders over $25"
        );

        createTierBenefit(
                MembershipTier.GOLD,
                BenefitType.FREE_SHIPPING,
                null,
                null,
                new BigDecimal("75.00"), // Free shipping on $75+ orders
                "Free shipping on orders over $75"
        );

        createTierBenefit(
                MembershipTier.GOLD,
                BenefitType.BIRTHDAY_BONUS,
                null,
                null,
                null,
                "200 bonus points on birthday"
        );

        createTierBenefit(
                MembershipTier.GOLD,
                BenefitType.PRIORITY_SUPPORT,
                null,
                null,
                null,
                "Priority customer support access"
        );

        // PLATINUM TIER BENEFITS
        createTierBenefit(
                MembershipTier.PLATINUM,
                BenefitType.DISCOUNT,
                new BigDecimal("7.00"), // 7% discount
                new BigDecimal("100.00"), // Max $100 discount
                new BigDecimal("25.00"), // Min $25 order
                "7% discount on orders over $25"
        );

        createTierBenefit(
                MembershipTier.PLATINUM,
                BenefitType.FREE_SHIPPING,
                null,
                null,
                new BigDecimal("50.00"), // Free shipping on $50+ orders
                "Free shipping on orders over $50"
        );

        createTierBenefit(
                MembershipTier.PLATINUM,
                BenefitType.BIRTHDAY_BONUS,
                null,
                null,
                null,
                "300 bonus points on birthday"
        );

        createTierBenefit(
                MembershipTier.PLATINUM,
                BenefitType.PRIORITY_SUPPORT,
                null,
                null,
                null,
                "Priority customer support with dedicated line"
        );

        createTierBenefit(
                MembershipTier.PLATINUM,
                BenefitType.EXCLUSIVE_ACCESS,
                null,
                null,
                null,
                "Early access to sales and new products"
        );

        // DIAMOND TIER BENEFITS
        createTierBenefit(
                MembershipTier.DIAMOND,
                BenefitType.DISCOUNT,
                new BigDecimal("10.00"), // 10% discount
                new BigDecimal("200.00"), // Max $200 discount
                null, // No minimum order
                "10% discount on all orders"
        );

        createTierBenefit(
                MembershipTier.DIAMOND,
                BenefitType.FREE_SHIPPING,
                null,
                null,
                null, // Free shipping on all orders
                "Free shipping on all orders"
        );

        createTierBenefit(
                MembershipTier.DIAMOND,
                BenefitType.BIRTHDAY_BONUS,
                null,
                null,
                null,
                "500 bonus points on birthday"
        );

        createTierBenefit(
                MembershipTier.DIAMOND,
                BenefitType.PRIORITY_SUPPORT,
                null,
                null,
                null,
                "Dedicated account manager and 24/7 support"
        );

        createTierBenefit(
                MembershipTier.DIAMOND,
                BenefitType.EXCLUSIVE_ACCESS,
                null,
                null,
                null,
                "VIP access to exclusive events and products"
        );

        createTierBenefit(
                MembershipTier.DIAMOND,
                BenefitType.POINT_MULTIPLIER,
                new BigDecimal("2.00"), // 2x points multiplier
                null,
                null,
                "Double points on all purchases"
        );

        log.info("✅ Created tier benefits for all membership levels");
    }

    /**
     * Create default loyalty rewards that customers can redeem
     */
    private void createDefaultLoyaltyRewards() {
        log.info("🎁 Creating default loyalty rewards...");

        // Check if rewards already exist
        if (loyaltyRewardRepository.count() > 0) {
            log.info("⏭️ Loyalty rewards already exist, skipping creation");
            return;
        }

        // LOW-COST REWARDS (100-500 points)
        createLoyaltyReward(
                "Free Shipping Coupon",
                "Get free shipping on your next order (up to $9.99 value)",
                100,
                true,
                90 // 90 days expiry
        );

        createLoyaltyReward(
                "$2 Off Coupon",
                "Get $2 off your next purchase (minimum $20 order)",
                150,
                true,
                60
        );

        createLoyaltyReward(
                "$5 Off Coupon",
                "Get $5 off your next purchase (minimum $50 order)",
                250,
                true,
                60
        );

        createLoyaltyReward(
                "Birthday Month Bonus",
                "Extra 50 bonus points during your birthday month",
                300,
                true,
                365
        );

        // MEDIUM REWARDS (500-1500 points)
        createLoyaltyReward(
                "$10 Gift Card",
                "Redeem for a $10 gift card to use on any purchase",
                500,
                true,
                180
        );

        createLoyaltyReward(
                "15% Off Coupon",
                "Get 15% off your next purchase (maximum $30 discount)",
                750,
                true,
                45
        );

        createLoyaltyReward(
                "$20 Gift Card",
                "Redeem for a $20 gift card to use on any purchase",
                1000,
                true,
                180
        );

        createLoyaltyReward(
                "Free Express Shipping",
                "Get free express shipping on your next order (up to $19.99 value)",
                1200,
                true,
                90
        );

        // HIGH-VALUE REWARDS (1500+ points)
        createLoyaltyReward(
                "$50 Gift Card",
                "Redeem for a $50 gift card to use on any purchase",
                2500,
                true,
                365
        );

        createLoyaltyReward(
                "25% Off Coupon",
                "Get 25% off your next purchase (maximum $75 discount)",
                3000,
                true,
                30
        );

        createLoyaltyReward(
                "$100 Gift Card",
                "Redeem for a $100 gift card to use on any purchase",
                5000,
                true,
                365
        );

        createLoyaltyReward(
                "VIP Customer Status",
                "Upgrade to VIP status for 6 months with exclusive perks",
                7500,
                true,
                180
        );

        // EXCLUSIVE/SEASONAL REWARDS
        createLoyaltyReward(
                "Early Access Pass",
                "Get 48-hour early access to sales and new product launches",
                1500,
                true,
                120
        );

        createLoyaltyReward(
                "Personal Shopping Session",
                "Get a 1-hour personal shopping consultation (Diamond tier only)",
                10000,
                true,
                90
        );

        // SEASONAL/LIMITED TIME (can be activated/deactivated)
        createLoyaltyReward(
                "Holiday Gift Wrapping",
                "Free premium gift wrapping service for holiday orders",
                200,
                false, // Inactive by default - activate during holidays
                30
        );

        createLoyaltyReward(
                "Summer Sale Preview",
                "Exclusive 3-day preview access to summer sale items",
                800,
                false, // Inactive by default - activate before summer
                14
        );

        log.info("✅ Created {} default loyalty rewards", loyaltyRewardRepository.count());
    }

    /**
     * Helper method to create a tier benefit
     */
    private void createTierBenefit(MembershipTier tier, BenefitType benefitType,
                                   BigDecimal discountPercentage, BigDecimal maxDiscountAmount,
                                   BigDecimal minOrderAmount, String description) {

        TierBenefit benefit = TierBenefit.builder()
                .tier(tier)
                .benefitType(benefitType)
                .discountPercentage(discountPercentage)
                .maxDiscountAmount(maxDiscountAmount)
                .minOrderAmount(minOrderAmount)
                .benefitConfig("{\"description\":\"" + description + "\"}")
                .active(true)
                .build();

        tierBenefitRepository.save(benefit);
        log.debug("➕ Created {} benefit for {} tier: {}", benefitType, tier, description);
    }

    /**
     * Helper method to create a loyalty reward
     */
    private void createLoyaltyReward(String name, String description, int pointsCost,
                                     boolean isActive, int expiryDays) {

        LoyaltyReward reward = new LoyaltyReward();
        reward.setId(UUID.randomUUID());
        reward.setName(name);
        reward.setDescription(description);
        reward.setPointsCost(pointsCost);
        reward.setActive(isActive);
        reward.setExpiryDays(expiryDays);

        loyaltyRewardRepository.save(reward);
        log.debug("🎁 Created reward: {} ({} points, {} days expiry)", name, pointsCost, expiryDays);
    }
}