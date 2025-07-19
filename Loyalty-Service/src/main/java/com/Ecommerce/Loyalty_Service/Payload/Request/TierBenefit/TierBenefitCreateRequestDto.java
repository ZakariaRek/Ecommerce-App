package com.Ecommerce.Loyalty_Service.Payload.Request.TierBenefit;

import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new tier benefit")
public class TierBenefitCreateRequestDto {

    @NotNull(message = "Membership tier is required")
    @Schema(description = "Membership tier", example = "GOLD")
    private MembershipTier tier;

    @NotNull(message = "Benefit type is required")
    @Schema(description = "Type of benefit", example = "DISCOUNT")
    private BenefitType benefitType;

    @Schema(description = "JSON configuration for the benefit",
            example = "{\"description\":\"5% discount on orders over $25\"}")
    private String benefitConfig;

    @DecimalMin(value = "0.01", message = "Discount percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
    @Schema(description = "Discount percentage (for discount benefits)", example = "5.00")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0.01", message = "Maximum discount amount must be greater than 0")
    @Schema(description = "Maximum discount amount", example = "50.00")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
    @Schema(description = "Minimum order amount for benefit eligibility", example = "25.00")
    private BigDecimal minOrderAmount;
}
