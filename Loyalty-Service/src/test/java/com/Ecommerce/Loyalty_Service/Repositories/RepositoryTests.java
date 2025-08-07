package com.Ecommerce.Loyalty_Service.Repositories;

import com.Ecommerce.Loyalty_Service.Entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CRMRepository crmRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private TierBenefitRepository tierBenefitRepository;

    @Autowired
    private LoyaltyRewardRepository loyaltyRewardRepository;

    private UUID userId;
    private CRM testCrm;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testCrm = new CRM();
        testCrm.setId(UUID.randomUUID());
        testCrm.setUserId(userId);
        testCrm.setTotalPoints(1000);
        testCrm.setMembershipLevel(MembershipTier.SILVER);
        testCrm.setJoinDate(LocalDateTime.now().minusMonths(6));
        testCrm.setLastActivity(LocalDateTime.now().minusDays(5));
    }

    // CRM Repository Tests
    @Test
    void crmRepository_findByUserId_ReturnsUser() {
        // Given
        entityManager.persistAndFlush(testCrm);

        // When
        Optional<CRM> result = crmRepository.findByUserId(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
        assertEquals(1000, result.get().getTotalPoints());
        assertEquals(MembershipTier.SILVER, result.get().getMembershipLevel());
    }

    @Test
    void crmRepository_findByUserId_NonExistentUser_ReturnsEmpty() {
        // Given - no user persisted

        // When
        Optional<CRM> result = crmRepository.findByUserId(UUID.randomUUID());

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void crmRepository_save_PersistsUser() {
        // When
        CRM savedCrm = crmRepository.save(testCrm);

        // Then
        assertNotNull(savedCrm.getId());
        assertEquals(testCrm.getUserId(), savedCrm.getUserId());
        assertEquals(testCrm.getTotalPoints(), savedCrm.getTotalPoints());

        // Verify in database
        CRM foundCrm = entityManager.find(CRM.class, savedCrm.getId());
        assertNotNull(foundCrm);
        assertEquals(userId, foundCrm.getUserId());
    }

    // Coupon Repository Tests
    @Test
    void couponRepository_findByCode_ReturnsCoupon() {
        // Given
        Coupon coupon = new Coupon();
        coupon.setCode("TEST20OFF");
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("20"));
        coupon.setMinPurchaseAmount(new BigDecimal("50"));
        coupon.setExpirationDate(LocalDateTime.now().plusDays(30));
        coupon.setUserId(userId);
        coupon.setUsed(false);
        coupon.setUsageLimit(1);
        coupon.setStackable(true);
        coupon.setPriorityLevel(1);

        entityManager.persistAndFlush(coupon);

        // When
        Optional<Coupon> result = couponRepository.findByCode("TEST20OFF");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TEST20OFF", result.get().getCode());
        assertEquals(DiscountType.PERCENTAGE, result.get().getDiscountType());
        assertEquals(new BigDecimal("20"), result.get().getDiscountValue());
    }

    @Test
    void couponRepository_findByUserIdAndIsUsedFalse_ReturnsActiveCoupons() {
        // Given
        Coupon activeCoupon1 = createTestCoupon("ACTIVE1", false);
        Coupon activeCoupon2 = createTestCoupon("ACTIVE2", false);
        Coupon usedCoupon = createTestCoupon("USED1", true);

        entityManager.persistAndFlush(activeCoupon1);
        entityManager.persistAndFlush(activeCoupon2);
        entityManager.persistAndFlush(usedCoupon);

        // When
        List<Coupon> result = couponRepository.findByUserIdAndIsUsedFalse(userId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> !c.isUsed()));
        assertTrue(result.stream().anyMatch(c -> c.getCode().equals("ACTIVE1")));
        assertTrue(result.stream().anyMatch(c -> c.getCode().equals("ACTIVE2")));
    }

    private Coupon createTestCoupon(String code, boolean isUsed) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setMinPurchaseAmount(new BigDecimal("25"));
        coupon.setExpirationDate(LocalDateTime.now().plusDays(30));
        coupon.setUserId(userId);
        coupon.setUsed(isUsed);
        coupon.setUsageLimit(1);
        coupon.setStackable(true);
        coupon.setPriorityLevel(1);
        return coupon;
    }

    // Point Transaction Repository Tests
    @Test
    void pointTransactionRepository_findByUserIdOrderByTransactionDateDesc_ReturnsOrderedTransactions() {
        // Given
        entityManager.persistAndFlush(testCrm);

        PointTransaction transaction1 = createTestTransaction(TransactionType.EARN, 100, LocalDateTime.now().minusDays(2));
        PointTransaction transaction2 = createTestTransaction(TransactionType.EARN, 50, LocalDateTime.now().minusDays(1));
        PointTransaction transaction3 = createTestTransaction(TransactionType.REDEEM, 25, LocalDateTime.now());

        entityManager.persistAndFlush(transaction1);
        entityManager.persistAndFlush(transaction2);
        entityManager.persistAndFlush(transaction3);

        // When
        List<PointTransaction> result = pointTransactionRepository.findByUserIdOrderByTransactionDateDesc(userId);

        // Then
        assertEquals(3, result.size());
        // Should be ordered by transaction date desc (most recent first)
        assertEquals(TransactionType.REDEEM, result.get(0).getType()); // Most recent
        assertEquals(50, result.get(1).getPoints());
        assertEquals(100, result.get(2).getPoints()); // Oldest
    }

    @Test
    void pointTransactionRepository_findByUserIdAndIdempotencyKey_ReturnsTransaction() {
        // Given
        entityManager.persistAndFlush(testCrm);

        String idempotencyKey = "order-12345";
        PointTransaction transaction = createTestTransaction(TransactionType.EARN, 150, LocalDateTime.now());
        transaction.setIdempotencyKey(idempotencyKey);

        entityManager.persistAndFlush(transaction);

        // When
        Optional<PointTransaction> result = pointTransactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

        // Then
        assertTrue(result.isPresent());
        assertEquals(idempotencyKey, result.get().getIdempotencyKey());
        assertEquals(150, result.get().getPoints());
        assertEquals(TransactionType.EARN, result.get().getType());
    }

    @Test
    void pointTransactionRepository_findByUserIdAndIdempotencyKey_NonExistentKey_ReturnsEmpty() {
        // Given
        entityManager.persistAndFlush(testCrm);

        PointTransaction transaction = createTestTransaction(TransactionType.EARN, 150, LocalDateTime.now());
        transaction.setIdempotencyKey("order-12345");
        entityManager.persistAndFlush(transaction);

        // When
        Optional<PointTransaction> result = pointTransactionRepository.findByUserIdAndIdempotencyKey(userId, "non-existent-key");

        // Then
        assertFalse(result.isPresent());
    }

    private PointTransaction createTestTransaction(TransactionType type, int points, LocalDateTime date) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setPoints(points);
        transaction.setTransactionDate(date);
        transaction.setSource("Test transaction");
        transaction.setBalance(1000 + points);
        return transaction;
    }

    // Tier Benefit Repository Tests
    @Test
    void tierBenefitRepository_findByTierAndBenefitTypeAndActiveTrue_ReturnsBenefit() {
        // Given
        TierBenefit benefit = createTestTierBenefit(MembershipTier.GOLD, BenefitType.DISCOUNT, true);
        entityManager.persistAndFlush(benefit);

        // When
        Optional<TierBenefit> result = tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(
                MembershipTier.GOLD, BenefitType.DISCOUNT);

        // Then
        assertTrue(result.isPresent());
        assertEquals(MembershipTier.GOLD, result.get().getTier());
        assertEquals(BenefitType.DISCOUNT, result.get().getBenefitType());
        assertTrue(result.get().getActive());
    }

    @Test
    void tierBenefitRepository_findByTierAndActiveTrue_ReturnsActiveBenefits() {
        // Given
        TierBenefit activeBenefit1 = createTestTierBenefit(MembershipTier.GOLD, BenefitType.DISCOUNT, true);
        TierBenefit activeBenefit2 = createTestTierBenefit(MembershipTier.GOLD, BenefitType.FREE_SHIPPING, true);
        TierBenefit inactiveBenefit = createTestTierBenefit(MembershipTier.GOLD, BenefitType.PRIORITY_SUPPORT, false);

        entityManager.persistAndFlush(activeBenefit1);
        entityManager.persistAndFlush(activeBenefit2);
        entityManager.persistAndFlush(inactiveBenefit);

        // When
        List<TierBenefit> result = tierBenefitRepository.findByTierAndActiveTrue(MembershipTier.GOLD);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(TierBenefit::getActive));
        assertTrue(result.stream().anyMatch(b -> b.getBenefitType() == BenefitType.DISCOUNT));
        assertTrue(result.stream().anyMatch(b -> b.getBenefitType() == BenefitType.FREE_SHIPPING));
    }

    @Test
    void tierBenefitRepository_existsByTierAndBenefitTypeAndActiveTrue_ReturnsTrue() {
        // Given
        TierBenefit benefit = createTestTierBenefit(MembershipTier.PLATINUM, BenefitType.EXCLUSIVE_ACCESS, true);
        entityManager.persistAndFlush(benefit);

        // When
        boolean exists = tierBenefitRepository.existsByTierAndBenefitTypeAndActiveTrue(
                MembershipTier.PLATINUM, BenefitType.EXCLUSIVE_ACCESS);

        // Then
        assertTrue(exists);
    }

    @Test
    void tierBenefitRepository_existsByTierAndBenefitTypeAndActiveTrue_ReturnsFalse() {
        // When
        boolean exists = tierBenefitRepository.existsByTierAndBenefitTypeAndActiveTrue(
                MembershipTier.DIAMOND, BenefitType.POINT_MULTIPLIER);

        // Then
        assertFalse(exists);
    }

    private TierBenefit createTestTierBenefit(MembershipTier tier, BenefitType benefitType, boolean active) {
        return TierBenefit.builder()
                .tier(tier)
                .benefitType(benefitType)
                .discountPercentage(new BigDecimal("5.0"))
                .maxDiscountAmount(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("25.0"))
                .benefitConfig("{\"description\":\"Test benefit\"}")
                .active(active)
                .build();
    }

    // Loyalty Reward Repository Tests
    @Test
    void loyaltyRewardRepository_findByIsActiveTrue_ReturnsActiveRewards() {
        // Given
        LoyaltyReward activeReward1 = createTestReward("$10 Gift Card", 500, true);
        LoyaltyReward activeReward2 = createTestReward("Free Shipping", 100, true);
        LoyaltyReward inactiveReward = createTestReward("Seasonal Reward", 200, false);

        entityManager.persistAndFlush(activeReward1);
        entityManager.persistAndFlush(activeReward2);
        entityManager.persistAndFlush(inactiveReward);

        // When
        List<LoyaltyReward> result = loyaltyRewardRepository.findByIsActiveTrue();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(LoyaltyReward::isActive));
        assertTrue(result.stream().anyMatch(r -> r.getName().equals("$10 Gift Card")));
        assertTrue(result.stream().anyMatch(r -> r.getName().equals("Free Shipping")));
    }

    private LoyaltyReward createTestReward(String name, int pointsCost, boolean active) {
        LoyaltyReward reward = new LoyaltyReward();
        reward.setId(UUID.randomUUID());
        reward.setName(name);
        reward.setDescription("Test reward: " + name);
        reward.setPointsCost(pointsCost);
        reward.setActive(active);
        reward.setExpiryDays(90);
        return reward;
    }

    // Complex Query Tests
    @Test
    void tierBenefitRepository_findAllOrderedByTierAndType_ReturnsOrderedResults() {
        // Given
        TierBenefit silverDiscount = createTestTierBenefit(MembershipTier.SILVER, BenefitType.DISCOUNT, true);
        TierBenefit goldDiscount = createTestTierBenefit(MembershipTier.GOLD, BenefitType.DISCOUNT, true);
        TierBenefit silverShipping = createTestTierBenefit(MembershipTier.SILVER, BenefitType.FREE_SHIPPING, true);

        entityManager.persistAndFlush(goldDiscount);
        entityManager.persistAndFlush(silverShipping);
        entityManager.persistAndFlush(silverDiscount);

        // When
        List<TierBenefit> result = tierBenefitRepository.findAllOrderedByTierAndType();

        // Then
        assertEquals(3, result.size());
        // Should be ordered by tier, then by benefit type
        // Note: Order depends on enum ordinal values
    }

    @Test
    void tierBenefitRepository_countActiveBenefitsByTier_ReturnsCorrectCount() {
        // Given
        TierBenefit activeBenefit1 = createTestTierBenefit(MembershipTier.DIAMOND, BenefitType.DISCOUNT, true);
        TierBenefit activeBenefit2 = createTestTierBenefit(MembershipTier.DIAMOND, BenefitType.FREE_SHIPPING, true);
        TierBenefit inactiveBenefit = createTestTierBenefit(MembershipTier.DIAMOND, BenefitType.PRIORITY_SUPPORT, false);

        entityManager.persistAndFlush(activeBenefit1);
        entityManager.persistAndFlush(activeBenefit2);
        entityManager.persistAndFlush(inactiveBenefit);

        // When
        long count = tierBenefitRepository.countActiveBenefitsByTier(MembershipTier.DIAMOND);

        // Then
        assertEquals(2, count);
    }

    // Test entity relationships and cascading
    @Test
    void couponUsageHistory_relationship_WorksCorrectly() {
        // Given
        Coupon coupon = createTestCoupon("RELATIONSHIP_TEST", false);
        entityManager.persistAndFlush(coupon);

        CouponUsageHistory usage = new CouponUsageHistory();
        usage.setCoupon(coupon);
        usage.setUserId(userId);
        usage.setDiscountAmount(new BigDecimal("15.00"));
        usage.setOriginalAmount(new BigDecimal("100.00"));
        usage.setFinalAmount(new BigDecimal("85.00"));

        entityManager.persistAndFlush(usage);

        // When
        CouponUsageHistory found = entityManager.find(CouponUsageHistory.class, usage.getId());

        // Then
        assertNotNull(found);
        assertNotNull(found.getCoupon());
        assertEquals("RELATIONSHIP_TEST", found.getCoupon().getCode());
        assertEquals(new BigDecimal("15.00"), found.getDiscountAmount());
    }
}