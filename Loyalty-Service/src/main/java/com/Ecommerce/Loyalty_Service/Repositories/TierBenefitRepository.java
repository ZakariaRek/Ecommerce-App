package com.Ecommerce.Loyalty_Service.Repositories;

import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.TierBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TierBenefitRepository extends JpaRepository<TierBenefit, UUID> {

    /**
     * Find active benefit by tier and benefit type
     */
    Optional<TierBenefit> findByTierAndBenefitTypeAndActiveTrue(MembershipTier tier, BenefitType benefitType);

    /**
     * Find all benefits by tier and active status
     */
    List<TierBenefit> findByTierAndActiveTrue(MembershipTier tier);

    /**
     * Find all benefits by tier (regardless of active status)
     */
    List<TierBenefit> findByTier(MembershipTier tier);

    /**
     * Find all active benefits
     */
    List<TierBenefit> findByActiveTrue();

    /**
     * Find all benefits by benefit type and active status
     */
    List<TierBenefit> findByBenefitTypeAndActiveTrue(BenefitType benefitType);

    /**
     * Find all benefits by benefit type (regardless of active status)
     */
    List<TierBenefit> findByBenefitType(BenefitType benefitType);

    /**
     * Count active benefits by tier
     */
    @Query("SELECT COUNT(tb) FROM TierBenefit tb WHERE tb.tier = :tier AND tb.active = true")
    long countActiveBenefitsByTier(@Param("tier") MembershipTier tier);

    /**
     * Count total benefits by tier
     */
    long countByTier(MembershipTier tier);

    /**
     * Find benefits by tier ordered by benefit type
     */
    List<TierBenefit> findByTierOrderByBenefitType(MembershipTier tier);

    /**
     * Find all benefits ordered by tier and benefit type
     */
    @Query("SELECT tb FROM TierBenefit tb ORDER BY tb.tier, tb.benefitType")
    List<TierBenefit> findAllOrderedByTierAndType();

    /**
     * Check if a specific combination exists
     */
    boolean existsByTierAndBenefitTypeAndActiveTrue(MembershipTier tier, BenefitType benefitType);
}