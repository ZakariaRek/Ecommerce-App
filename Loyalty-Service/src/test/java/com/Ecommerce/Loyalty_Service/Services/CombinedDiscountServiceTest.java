package com.Ecommerce.Loyalty_Service.Services.Kafka;

import com.Ecommerce.Loyalty_Service.Entities.*;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Request.CombinedDiscountRequest;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CombinedDiscountResponse;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CouponDiscountDetail;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CouponValidationResponse;
import com.Ecommerce.Loyalty_Service.Repositories.TierBenefitRepository;
import com.Ecommerce.Loyalty_Service.Services.CRMService;
import com.Ecommerce.Loyalty_Service.Services.CouponValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CombinedDiscountServiceTest {

    @Mock
    private CRMService crmService;

    @Mock
    private CouponValidationService couponValidationService;

    @Mock
    private TierBenefitRepository tierBenefitRepository;

    @InjectMocks
    private CombinedDiscountService combinedDiscountService;

    private UUID userId;
    private UUID orderId;
    private CRM testCrm;
    private CombinedDiscountRequest testRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        testCrm = new CRM();
        testCrm.setUserId(userId);
        testCrm.setTotalPoints(2500);
        testCrm.setMembershipLevel(MembershipTier.GOLD);

        testRequest = CombinedDiscountRequest.builder()
                .correlationId("test-correlation-123")
                .userId(userId)
                .orderId(orderId)
                .originalAmount(new BigDecimal("200.00"))
                .productDiscount(new BigDecimal("20.00"))
                .orderLevelDiscount(new BigDecimal("10.00"))
                .amountAfterOrderDiscounts(new BigDecimal("170.00"))
                .couponCodes(Arrays.asList("SAVE15", "SUMMER10"))
                .totalItems(3)
                .customerTier("GOLD")
                .build();
    }

    @Test
    void calculateCombinedDiscounts_WithValidCouponsAndTierBenefit_ReturnsCompleteResponse() {
        // Given
        // Mock coupon validation response
        CouponDiscountDetail coupon1 = CouponDiscountDetail.builder()
                .couponCode("SAVE15")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("15"))
                .calculatedDiscount(new BigDecimal("25.50"))
                .build();

        CouponDiscountDetail coupon2 = CouponDiscountDetail.builder()
                .couponCode("SUMMER10")
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("10"))
                .calculatedDiscount(new BigDecimal("10.00"))
                .build();

        CouponValidationResponse couponResponse = CouponValidationResponse.builder()
                .totalDiscount(new BigDecimal("35.50"))
                .validCoupons(Arrays.asList(coupon1, coupon2))
                .success(true)
                .errors(Arrays.asList())
                .build();

        // Mock tier benefit
        TierBenefit goldBenefit = TierBenefit.builder()
                .tier(MembershipTier.GOLD)
                .benefitType(BenefitType.DISCOUNT)
                .discountPercentage(new BigDecimal("5.0"))
                .maxDiscountAmount(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("25.0"))
                .active(true)
                .build();

        when(couponValidationService.validateAndCalculateDiscount(
                eq(Arrays.asList("SAVE15", "SUMMER10")), eq(userId), eq(new BigDecimal("170.00"))))
                .thenReturn(couponResponse);

        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.GOLD, BenefitType.DISCOUNT))
                .thenReturn(Optional.of(goldBenefit));

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(testRequest);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("test-correlation-123", response.getCorrelationId());
        assertEquals(userId, response.getUserId());
        assertEquals(orderId, response.getOrderId());

        // Coupon discounts
        assertEquals(new BigDecimal("35.50"), response.getCouponDiscount());
        assertEquals(2, response.getValidCoupons().size());
        assertTrue(response.isCouponSuccess());

        // Tier discount (5% of remaining amount after coupons: (170 - 35.50) * 0.05 = 6.725)
        assertEquals(new BigDecimal("6.73"), response.getTierDiscount().setScale(2));
        assertEquals("GOLD", response.getMembershipTier());
        assertTrue(response.isTierDiscountSuccess());

        // Total calculations
        assertEquals(new BigDecimal("42.23"), response.getTotalDiscount().setScale(2));
        assertEquals(new BigDecimal("127.77"), response.getFinalAmount().setScale(2));

        // Breakdown should include all discount types
        assertNotNull(response.getBreakdown());
        assertTrue(response.getBreakdown().size() >= 2); // At least coupon + tier discounts
    }

    @Test
    void calculateCombinedDiscounts_NoCoupons_OnlyTierDiscount() {
        // Given
        CombinedDiscountRequest requestNoCoupons = CombinedDiscountRequest.builder()
                .correlationId("no-coupons-123")
                .userId(userId)
                .orderId(orderId)
                .originalAmount(new BigDecimal("100.00"))
                .productDiscount(new BigDecimal("0.00"))
                .orderLevelDiscount(new BigDecimal("0.00"))
                .amountAfterOrderDiscounts(new BigDecimal("100.00"))
                .couponCodes(null) // No coupons
                .totalItems(1)
                .build();

        TierBenefit goldBenefit = TierBenefit.builder()
                .tier(MembershipTier.GOLD)
                .benefitType(BenefitType.DISCOUNT)
                .discountPercentage(new BigDecimal("5.0"))
                .maxDiscountAmount(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("25.0"))
                .active(true)
                .build();

        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.GOLD, BenefitType.DISCOUNT))
                .thenReturn(Optional.of(goldBenefit));

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(requestNoCoupons);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(BigDecimal.ZERO, response.getCouponDiscount());
        assertTrue(response.getValidCoupons().isEmpty());

        // Tier discount: 100 * 0.05 = 5.00
        assertEquals(new BigDecimal("5.00"), response.getTierDiscount());
        assertEquals(new BigDecimal("5.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("95.00"), response.getFinalAmount());
    }

    @Test
    void calculateCombinedDiscounts_NoTierBenefit_OnlyCouponDiscount() {
        // Given
        CouponValidationResponse couponResponse = CouponValidationResponse.builder()
                .totalDiscount(new BigDecimal("20.00"))
                .validCoupons(Arrays.asList(
                        CouponDiscountDetail.builder()
                                .couponCode("SAVE20")
                                .discountType("FIXED_AMOUNT")
                                .discountValue(new BigDecimal("20"))
                                .calculatedDiscount(new BigDecimal("20.00"))
                                .build()
                ))
                .success(true)
                .errors(Arrays.asList())
                .build();

        when(couponValidationService.validateAndCalculateDiscount(any(), eq(userId), any()))
                .thenReturn(couponResponse);
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.GOLD, BenefitType.DISCOUNT))
                .thenReturn(Optional.empty()); // No tier benefit

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(testRequest);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(new BigDecimal("20.00"), response.getCouponDiscount());
        assertEquals(BigDecimal.ZERO, response.getTierDiscount());
        assertEquals(new BigDecimal("20.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("150.00"), response.getFinalAmount()); // 170 - 20
    }

    @Test
    void calculateCombinedDiscounts_InvalidCoupons_ReturnsCouponErrors() {
        // Given
        CouponValidationResponse couponResponse = CouponValidationResponse.builder()
                .totalDiscount(BigDecimal.ZERO)
                .validCoupons(Arrays.asList())
                .success(false)
                .errors(Arrays.asList("Coupon SAVE15 has expired", "Coupon SUMMER10 not found"))
                .build();

        TierBenefit goldBenefit = TierBenefit.builder()
                .tier(MembershipTier.GOLD)
                .benefitType(BenefitType.DISCOUNT)
                .discountPercentage(new BigDecimal("5.0"))
                .maxDiscountAmount(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("25.0"))
                .active(true)
                .build();

        when(couponValidationService.validateAndCalculateDiscount(any(), eq(userId), any()))
                .thenReturn(couponResponse);
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.GOLD, BenefitType.DISCOUNT))
                .thenReturn(Optional.of(goldBenefit));

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(testRequest);

        // Then
        assertTrue(response.isSuccess()); // Overall success despite coupon issues
        assertFalse(response.isCouponSuccess());
        assertTrue(response.isTierDiscountSuccess());

        assertEquals(BigDecimal.ZERO, response.getCouponDiscount());
        assertEquals(2, response.getCouponErrors().size());
        assertTrue(response.getCouponErrors().contains("Coupon SAVE15 has expired"));

        // Should still apply tier discount
        assertEquals(new BigDecimal("8.50"), response.getTierDiscount()); // 170 * 0.05
    }

    @Test
    void calculateCombinedDiscounts_TierDiscountBelowMinimum_NoTierDiscount() {
        // Given
        CombinedDiscountRequest smallOrderRequest = CombinedDiscountRequest.builder()
                .correlationId("small-order-123")
                .userId(userId)
                .orderId(orderId)
                .originalAmount(new BigDecimal("20.00"))
                .productDiscount(BigDecimal.ZERO)
                .orderLevelDiscount(BigDecimal.ZERO)
                .amountAfterOrderDiscounts(new BigDecimal("20.00"))
                .couponCodes(null)
                .totalItems(1)
                .build();

        TierBenefit goldBenefit = TierBenefit.builder()
                .tier(MembershipTier.GOLD)
                .benefitType(BenefitType.DISCOUNT)
                .discountPercentage(new BigDecimal("5.0"))
                .maxDiscountAmount(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("25.0")) // Minimum $25
                .active(true)
                .build();

        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.GOLD, BenefitType.DISCOUNT))
                .thenReturn(Optional.of(goldBenefit));

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(smallOrderRequest);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(BigDecimal.ZERO, response.getTierDiscount()); // Below minimum
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
        assertEquals(new BigDecimal("20.00"), response.getFinalAmount()); // No discount applied
    }

    @Test
    void calculateCombinedDiscounts_TierDiscountCappedAtMaximum() {
        // Given
        CombinedDiscountRequest largeOrderRequest = CombinedDiscountRequest.builder()
                .correlationId("large-order-123")
                .userId(userId)
                .orderId(orderId)
                .originalAmount(new BigDecimal("2000.00"))
                .productDiscount(BigDecimal.ZERO)
                .orderLevelDiscount(BigDecimal.ZERO)
                .amountAfterOrderDiscounts(new BigDecimal("2000.00"))
                .couponCodes(null)
                .totalItems(10)
                .build();

        TierBenefit goldBenefit = TierBenefit.builder()
                .tier(MembershipTier.GOLD)
                .benefitType(BenefitType.DISCOUNT)
                .discountPercentage(new BigDecimal("5.0"))
                .maxDiscountAmount(new BigDecimal("50.0")) // Cap at $50
                .minOrderAmount(new BigDecimal("25.0"))
                .active(true)
                .build();

        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.GOLD, BenefitType.DISCOUNT))
                .thenReturn(Optional.of(goldBenefit));

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(largeOrderRequest);

        // Then
        assertTrue(response.isSuccess());
        assertEquals(new BigDecimal("50.0"), response.getTierDiscount()); // Capped at maximum
        assertEquals(new BigDecimal("50.0"), response.getTotalDiscount());
        assertEquals(new BigDecimal("1950.00"), response.getFinalAmount());
    }

    @Test
    void calculateCombinedDiscounts_UserNotFound_ReturnsError() {
        // Given
        when(crmService.getByUserId(userId)).thenThrow(new RuntimeException("User not found in loyalty system"));

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(testRequest);

        // Then
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("Failed to calculate combined discounts"));
        assertEquals(BigDecimal.ZERO, response.getCouponDiscount());
        assertEquals(BigDecimal.ZERO, response.getTierDiscount());
        assertEquals(BigDecimal.ZERO, response.getTotalDiscount());
    }

    @Test
    void calculateCombinedDiscounts_BronzeUser_NoTierBenefit() {
        // Given
        testCrm.setMembershipLevel(MembershipTier.BRONZE);

        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.BRONZE, BenefitType.DISCOUNT))
                .thenReturn(Optional.empty());

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(testRequest);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("BRONZE", response.getMembershipTier());
        assertEquals(BigDecimal.ZERO, response.getTierDiscount());
        assertTrue(response.isTierDiscountSuccess()); // Success even with no benefit
    }

    @Test
    void calculateCombinedDiscounts_DetailedBreakdown_IncludesAllDiscountTypes() {
        // Given
        CouponValidationResponse couponResponse = CouponValidationResponse.builder()
                .totalDiscount(new BigDecimal("30.00"))
                .validCoupons(Arrays.asList(
                        CouponDiscountDetail.builder()
                                .couponCode("SAVE30")
                                .discountType("FIXED_AMOUNT")
                                .discountValue(new BigDecimal("30"))
                                .calculatedDiscount(new BigDecimal("30.00"))
                                .build()
                ))
                .success(true)
                .errors(Arrays.asList())
                .build();

        TierBenefit goldBenefit = TierBenefit.builder()
                .tier(MembershipTier.GOLD)
                .benefitType(BenefitType.DISCOUNT)
                .discountPercentage(new BigDecimal("5.0"))
                .maxDiscountAmount(new BigDecimal("50.0"))
                .minOrderAmount(new BigDecimal("25.0"))
                .active(true)
                .build();

        when(couponValidationService.validateAndCalculateDiscount(any(), eq(userId), any()))
                .thenReturn(couponResponse);
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(tierBenefitRepository.findByTierAndBenefitTypeAndActiveTrue(MembershipTier.GOLD, BenefitType.DISCOUNT))
                .thenReturn(Optional.of(goldBenefit));

        // When
        CombinedDiscountResponse response = combinedDiscountService.calculateCombinedDiscounts(testRequest);

        // Then
        assertTrue(response.isSuccess());
        assertNotNull(response.getBreakdown());

        // Should have breakdown for: product discount, order-level discount, coupon discount, tier discount
        assertTrue(response.getBreakdown().size() >= 4);

        // Check that breakdown includes all discount sources
        List<String> discountTypes = response.getBreakdown().stream()
                .map(breakdown -> breakdown.getDiscountType())
                .toList();

        assertTrue(discountTypes.contains("PRODUCT"));
        assertTrue(discountTypes.contains("ORDER_LEVEL"));
        assertTrue(discountTypes.contains("LOYALTY_COUPON"));
        assertTrue(discountTypes.contains("TIER_BENEFIT"));
    }
}