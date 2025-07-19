package com.Ecommerce.Loyalty_Service.Payload.Response.TierBenefit;


import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Simplified tier benefit summary")
public class TierBenefitSummaryDto {

    @Schema(description = "Tier benefit ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Membership tier", example = "GOLD")
    private MembershipTier tier;

    @Schema(description = "Type of benefit", example = "DISCOUNT")
    private BenefitType benefitType;

    @Schema(description = "Discount percentage", example = "5.00")
    private BigDecimal discountPercentage;

    @Schema(description = "Maximum discount amount", example = "50.00")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "Minimum order amount", example = "25.00")
    private BigDecimal minOrderAmount;

    @Schema(description = "Whether the benefit is active", example = "true")
    private Boolean active;

    @Schema(description = "Human-readable benefit description",
            example = "5% discount on orders over $25 (max $50)")
    private String description;
}