package com.Ecommerce.Loyalty_Service.Mappers;

import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.TierBenefit;
import com.Ecommerce.Loyalty_Service.Payload.Request.TierBenefit.TierBenefitCreateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.TierBenefit.TierBenefitResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.TierBenefit.TierBenefitSummaryDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TierBenefitMapper {

    /**
     * Convert TierBenefit entity to TierBenefitResponseDto
     */
    public TierBenefitResponseDto toResponseDto(TierBenefit tierBenefit) {
        return TierBenefitResponseDto.builder()
                .id(tierBenefit.getId())
                .tier(tierBenefit.getTier())
                .benefitType(tierBenefit.getBenefitType())
                .benefitConfig(tierBenefit.getBenefitConfig())
                .discountPercentage(tierBenefit.getDiscountPercentage())
                .maxDiscountAmount(tierBenefit.getMaxDiscountAmount())
                .minOrderAmount(tierBenefit.getMinOrderAmount())
                .active(tierBenefit.getActive())
                .createdAt(tierBenefit.getCreatedAt())
                .updatedAt(tierBenefit.getUpdatedAt())
                .build();
    }

    /**
     * Convert TierBenefit entity to TierBenefitSummaryDto
     */
    public TierBenefitSummaryDto toSummaryDto(TierBenefit tierBenefit) {
        return TierBenefitSummaryDto.builder()
                .id(tierBenefit.getId())
                .tier(tierBenefit.getTier())
                .benefitType(tierBenefit.getBenefitType())
                .discountPercentage(tierBenefit.getDiscountPercentage())
                .maxDiscountAmount(tierBenefit.getMaxDiscountAmount())
                .minOrderAmount(tierBenefit.getMinOrderAmount())
                .active(tierBenefit.getActive())
                .description(generateBenefitDescription(tierBenefit))
                .build();
    }

    /**
     * Convert TierBenefitCreateRequestDto to TierBenefit entity
     */
    public TierBenefit toEntity(TierBenefitCreateRequestDto requestDto) {
        return TierBenefit.builder()
                .tier(requestDto.getTier())
                .benefitType(requestDto.getBenefitType())
                .benefitConfig(requestDto.getBenefitConfig())
                .discountPercentage(requestDto.getDiscountPercentage())
                .maxDiscountAmount(requestDto.getMaxDiscountAmount())
                .minOrderAmount(requestDto.getMinOrderAmount())
                .active(true) // New benefits are active by default
                .build();
    }

    /**
     * Generate a human-readable description for the benefit
     */
    private String generateBenefitDescription(TierBenefit tierBenefit) {
        BenefitType type = tierBenefit.getBenefitType();

        switch (type) {
            case DISCOUNT:
                StringBuilder discountDesc = new StringBuilder();
                if (tierBenefit.getDiscountPercentage() != null) {
                    discountDesc.append(tierBenefit.getDiscountPercentage()).append("% discount");
                }

                if (tierBenefit.getMinOrderAmount() != null &&
                        tierBenefit.getMinOrderAmount().compareTo(BigDecimal.ZERO) > 0) {
                    discountDesc.append(" on orders over $").append(tierBenefit.getMinOrderAmount());
                }

                if (tierBenefit.getMaxDiscountAmount() != null) {
                    discountDesc.append(" (max $").append(tierBenefit.getMaxDiscountAmount()).append(")");
                }

                return discountDesc.toString();

            case FREE_SHIPPING:
                if (tierBenefit.getMinOrderAmount() != null &&
                        tierBenefit.getMinOrderAmount().compareTo(BigDecimal.ZERO) > 0) {
                    return "Free shipping on orders over $" + tierBenefit.getMinOrderAmount();
                } else {
                    return "Free shipping on all orders";
                }

            case PRIORITY_SUPPORT:
                return "Priority customer support access";

            case EXCLUSIVE_ACCESS:
                return "Exclusive access to sales and new products";

            case BIRTHDAY_BONUS:
                return "Birthday bonus points";

            case POINT_MULTIPLIER:
                if (tierBenefit.getDiscountPercentage() != null) {
                    return tierBenefit.getDiscountPercentage() + "x points multiplier";
                } else {
                    return "Points multiplier";
                }

            default:
                return "Special benefit";
        }
    }

    /**
     * Generate tier benefit description with tier context
     */
    public String generateTierBenefitDescription(TierBenefit tierBenefit) {
        String baseDescription = generateBenefitDescription(tierBenefit);
        return tierBenefit.getTier().toString() + " tier: " + baseDescription;
    }

    /**
     * Check if benefit has monetary values
     */
    public boolean hasMonetaryValues(TierBenefit tierBenefit) {
        return tierBenefit.getDiscountPercentage() != null ||
                tierBenefit.getMaxDiscountAmount() != null ||
                tierBenefit.getMinOrderAmount() != null;
    }

    /**
     * Generate benefit value summary
     */
    public String generateBenefitValueSummary(TierBenefit tierBenefit) {
        StringBuilder summary = new StringBuilder();

        if (tierBenefit.getDiscountPercentage() != null) {
            summary.append("Discount: ").append(tierBenefit.getDiscountPercentage()).append("%");
        }

        if (tierBenefit.getMaxDiscountAmount() != null) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("Max: $").append(tierBenefit.getMaxDiscountAmount());
        }

        if (tierBenefit.getMinOrderAmount() != null &&
                tierBenefit.getMinOrderAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("Min Order: $").append(tierBenefit.getMinOrderAmount());
        }

        return summary.toString();
    }
}