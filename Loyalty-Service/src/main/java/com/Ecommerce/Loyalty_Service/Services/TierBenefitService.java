package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.TierBenefit;
import com.Ecommerce.Loyalty_Service.Repositories.TierBenefitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TierBenefitService {

    private final TierBenefitRepository tierBenefitRepository;

    /**
     * Get all tier benefits with pagination
     */
    public Page<TierBenefit> getAllTierBenefits(Pageable pageable) {
        log.info("Retrieving all tier benefits with pagination");
        return tierBenefitRepository.findAll(pageable);
    }

    /**
     * Get all tier benefits without pagination
     */
    public List<TierBenefit> getAllTierBenefits() {
        log.info("Retrieving all tier benefits");
        return tierBenefitRepository.findAll();
    }

    /**
     * Get tier benefit by ID
     */
    public TierBenefit getTierBenefitById(UUID id) {
        log.info("Retrieving tier benefit with ID: {}", id);
        return tierBenefitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tier benefit not found with ID: " + id));
    }

    /**
     * Get benefits by membership tier
     */
    public List<TierBenefit> getBenefitsByTier(MembershipTier tier) {
        log.info("Retrieving benefits for tier: {}", tier);
        return tierBenefitRepository.findByTierAndActiveTrue(tier);
    }

    /**
     * Get specific benefit for a tier and benefit type
     */
    public Optional<TierBenefit> getBenefitByTierAndType(MembershipTier tier, BenefitType benefitType) {
        log.info("Retrieving benefit for tier: {} and type: {}", tier, benefitType);
        return tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(tier, benefitType);
    }

    /**
     * Create a new tier benefit
     */
    @Transactional
    public TierBenefit createTierBenefit(MembershipTier tier, BenefitType benefitType,
                                         String benefitConfig, BigDecimal discountPercentage,
                                         BigDecimal maxDiscountAmount, BigDecimal minOrderAmount) {
        log.info("Creating new tier benefit for tier: {} and type: {}", tier, benefitType);

        // Check if benefit already exists for this tier and type
        Optional<TierBenefit> existingBenefit = tierBenefitRepository
                .findByTierAndBenefitTypeAndActiveTrue(tier, benefitType);

        if (existingBenefit.isPresent()) {
            throw new RuntimeException("Benefit already exists for tier " + tier + " and type " + benefitType);
        }

        TierBenefit tierBenefit = TierBenefit.builder()
                .tier(tier)
                .benefitType(benefitType)
                .benefitConfig(benefitConfig)
                .discountPercentage(discountPercentage)
                .maxDiscountAmount(maxDiscountAmount)
                .minOrderAmount(minOrderAmount)
                .active(true)
                .build();

        TierBenefit savedBenefit = tierBenefitRepository.save(tierBenefit);
        log.info("Created tier benefit with ID: {}", savedBenefit.getId());

        return savedBenefit;
    }

    /**
     * Update an existing tier benefit
     */
    @Transactional
    public TierBenefit updateTierBenefit(UUID id, String benefitConfig,
                                         BigDecimal discountPercentage, BigDecimal maxDiscountAmount,
                                         BigDecimal minOrderAmount, Boolean active) {
        log.info("Updating tier benefit with ID: {}", id);

        TierBenefit existingBenefit = getTierBenefitById(id);

        // Update fields if provided
        if (benefitConfig != null) {
            existingBenefit.setBenefitConfig(benefitConfig);
        }
        if (discountPercentage != null) {
            existingBenefit.setDiscountPercentage(discountPercentage);
        }
        if (maxDiscountAmount != null) {
            existingBenefit.setMaxDiscountAmount(maxDiscountAmount);
        }
        if (minOrderAmount != null) {
            existingBenefit.setMinOrderAmount(minOrderAmount);
        }
        if (active != null) {
            existingBenefit.setActive(active);
        }

        TierBenefit updatedBenefit = tierBenefitRepository.save(existingBenefit);
        log.info("Updated tier benefit with ID: {}", updatedBenefit.getId());

        return updatedBenefit;
    }

    /**
     * Activate a tier benefit
     */
    @Transactional
    public TierBenefit activateTierBenefit(UUID id) {
        log.info("Activating tier benefit with ID: {}", id);

        TierBenefit benefit = getTierBenefitById(id);
        benefit.setActive(true);

        TierBenefit activatedBenefit = tierBenefitRepository.save(benefit);
        log.info("Activated tier benefit with ID: {}", activatedBenefit.getId());

        return activatedBenefit;
    }

    /**
     * Get active tier benefits
     */
    public List<TierBenefit> getActiveTierBenefits() {
        log.info("Retrieving all active tier benefits");
        return tierBenefitRepository.findByActiveTrue();
    }

    /**
     * Get tier benefits summary (same as getting all active benefits)
     */
    public List<TierBenefit> getTierBenefitsSummary() {
        log.info("Retrieving tier benefits summary");
        return getAllActiveBenefits();
    }

    /**
     * Get tier benefits by tier
     */
    public List<TierBenefit> getTierBenefitsByTier(MembershipTier tier) {
        log.info("Retrieving benefits for tier: {}", tier);
        return getBenefitsByTier(tier);
    }

    /**
     * Get tier benefits by type
     */
    public List<TierBenefit> getTierBenefitsByType(BenefitType benefitType) {
        log.info("Retrieving benefits by type: {}", benefitType);
        return getBenefitsByType(benefitType);
    }

    /**
     * Get tier benefit by tier and type
     */
    public Optional<TierBenefit> getTierBenefit(MembershipTier tier, BenefitType benefitType) {
        log.info("Retrieving benefit for tier: {} and type: {}", tier, benefitType);
        return getBenefitByTierAndType(tier, benefitType);
    }

    /**
     * Validate benefit configuration
     */
    public boolean validateBenefitConfiguration(BenefitType benefitType, BigDecimal discountPercentage,
                                                BigDecimal maxDiscountAmount, BigDecimal minOrderAmount) {
        log.info("Validating benefit configuration for type: {}", benefitType);

        // Basic validation logic
        if (benefitType == BenefitType.DISCOUNT) {
            return discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0
                    && discountPercentage.compareTo(BigDecimal.valueOf(100)) <= 0;
        }

        // Add validation for other benefit types as needed
        return true;
    }

    /**
     * Deactivate a tier benefit
     */
    @Transactional
    public TierBenefit deactivateTierBenefit(UUID id) {
        log.info("Deactivating tier benefit with ID: {}", id);

        TierBenefit benefit = getTierBenefitById(id);
        benefit.setActive(false);

        TierBenefit deactivatedBenefit = tierBenefitRepository.save(benefit);
        log.info("Deactivated tier benefit with ID: {}", deactivatedBenefit.getId());

        return deactivatedBenefit;
    }

    /**
     * Delete a tier benefit (soft delete by deactivating)
     */
    @Transactional
    public void deleteTierBenefit(UUID id) {
        log.info("Deleting (deactivating) tier benefit with ID: {}", id);
        deactivateTierBenefit(id);
    }

    /**
     * Hard delete a tier benefit (use with caution)
     */
    @Transactional
    public void hardDeleteTierBenefit(UUID id) {
        log.warn("Hard deleting tier benefit with ID: {}", id);

        TierBenefit benefit = getTierBenefitById(id);
        tierBenefitRepository.delete(benefit);

        log.warn("Hard deleted tier benefit with ID: {}", id);
    }

    /**
     * Check if a specific benefit exists for a tier
     */
    public boolean benefitExistsForTier(MembershipTier tier, BenefitType benefitType) {
        return tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(tier, benefitType).isPresent();
    }

    /**
     * Get all active benefits
     */
    public List<TierBenefit> getAllActiveBenefits() {
        log.info("Retrieving all active tier benefits");
        return tierBenefitRepository.findByActiveTrue();
    }

    /**
     * Get benefits by benefit type across all tiers
     */
    public List<TierBenefit> getBenefitsByType(BenefitType benefitType) {
        log.info("Retrieving benefits by type: {}", benefitType);
        return tierBenefitRepository.findByBenefitTypeAndActiveTrue(benefitType);
    }
}