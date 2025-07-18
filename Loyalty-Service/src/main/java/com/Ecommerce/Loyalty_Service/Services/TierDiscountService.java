package com.Ecommerce.Loyalty_Service.Services;


import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.TierBenefit;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.TierDiscountResponse;
import com.Ecommerce.Loyalty_Service.Repositories.TierBenefitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TierDiscountService {

    private final CRMService crmService;
    private final TierBenefitRepository tierBenefitRepository;

    public TierDiscountResponse calculateTierDiscount(UUID userId, BigDecimal amount) {
        try {
            log.info("💎 LOYALTY SERVICE: Calculating tier discount for user {} with amount {}", userId, amount);

            CRM crm = crmService.getByUserId(userId);
            MembershipTier tier = crm.getMembershipLevel();

            log.info("💎 LOYALTY SERVICE: User {} has tier: {}", userId, tier);

            Optional<TierBenefit> benefitOpt = tierBenefitRepository
                    .findByTierAndBenefitTypeAndActiveTrue(tier, BenefitType.DISCOUNT);

            if (benefitOpt.isEmpty()) {
                log.info("💎 LOYALTY SERVICE: No discount benefit found for tier: {}", tier);
                return TierDiscountResponse.builder()
                        .userId(userId)
                        .tier(tier.toString())
                        .discountAmount(BigDecimal.ZERO)
                        .applicableAmount(amount)
                        .build();
            }

            TierBenefit benefit = benefitOpt.get();

            // Check minimum order amount
            if (benefit.getMinOrderAmount() != null &&
                    amount.compareTo(benefit.getMinOrderAmount()) < 0) {
                log.info("💎 LOYALTY SERVICE: Amount {} below minimum {} for tier {}",
                        amount, benefit.getMinOrderAmount(), tier);
                return TierDiscountResponse.builder()
                        .userId(userId)
                        .tier(tier.toString())
                        .discountAmount(BigDecimal.ZERO)
                        .applicableAmount(amount)
                        .build();
            }

            BigDecimal discountAmount = amount
                    .multiply(benefit.getDiscountPercentage().divide(new BigDecimal("100")));

            // Apply max discount limit
            if (benefit.getMaxDiscountAmount() != null &&
                    discountAmount.compareTo(benefit.getMaxDiscountAmount()) > 0) {
                discountAmount = benefit.getMaxDiscountAmount();
                log.info("💎 LOYALTY SERVICE: Discount capped at maximum: {}", discountAmount);
            }

            log.info("💎 LOYALTY SERVICE: Tier {} discount calculated: {}", tier, discountAmount);

            return TierDiscountResponse.builder()
                    .userId(userId)
                    .tier(tier.toString())
                    .discountAmount(discountAmount)
                    .applicableAmount(amount)
                    .maxDiscountAmount(benefit.getMaxDiscountAmount())
                    .discountPercentage(benefit.getDiscountPercentage())
                    .build();

        } catch (Exception e) {
            log.error("💎 LOYALTY SERVICE: Error calculating tier discount for user {}: {}", userId, e.getMessage());
            return TierDiscountResponse.builder()
                    .userId(userId)
                    .discountAmount(BigDecimal.ZERO)
                    .applicableAmount(amount)
                    .error("Failed to calculate tier discount: " + e.getMessage())
                    .build();
        }
    }
}