package com.Ecommerce.Loyalty_Service.Repositories;

import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.TierBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TierBenefitRepository extends JpaRepository<TierBenefit, UUID> {
    List<TierBenefit> findByTierAndActiveTrue(MembershipTier tier);
    List<TierBenefit> findByBenefitTypeAndActiveTrue(BenefitType benefitType);
    Optional<TierBenefit> findByTierAndBenefitTypeAndActiveTrue(MembershipTier tier, BenefitType benefitType);
}