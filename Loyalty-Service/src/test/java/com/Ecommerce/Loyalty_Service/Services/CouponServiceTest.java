package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.*;
import com.Ecommerce.Loyalty_Service.Repositories.CouponRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CouponKafkaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponKafkaService kafkaService;

    @Mock
    private PointTransactionService pointTransactionService;

    @Mock
    private CRMService crmService;

    @InjectMocks
    private CouponService couponService;

    private UUID userId;
    private CRM testCrm;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        // Setup CRM user
        testCrm = new CRM();
        testCrm.setId(UUID.randomUUID());
        testCrm.setUserId(userId);
        testCrm.setTotalPoints(1000);
        testCrm.setMembershipLevel(MembershipTier.SILVER);

        // Setup test coupon
        testCoupon = new Coupon();
        testCoupon.setId(UUID.randomUUID());
        testCoupon.setCode("TEST20OFF");
        testCoupon.setDiscountType(DiscountType.PERCENTAGE);
        testCoupon.setDiscountValue(new BigDecimal("20"));
        testCoupon.setMinPurchaseAmount(new BigDecimal("50"));
        testCoupon.setMaxDiscountAmount(new BigDecimal("100"));
        testCoupon.setExpirationDate(LocalDateTime.now().plusDays(30));
        testCoupon.setUserId(userId);
        testCoupon.setUsed(false);
        testCoupon.setUsageLimit(1);
        testCoupon.setStackable(true);
        testCoupon.setPriorityLevel(1);
    }

    @Test
    void getAllCoupons_ReturnsAllCoupons() {
        // Given
        List<Coupon> expectedCoupons = Arrays.asList(testCoupon);
        when(couponRepository.findAll()).thenReturn(expectedCoupons);

        // When
        List<Coupon> result = couponService.getAllCoupons();

        // Then
        assertEquals(1, result.size());
        assertEquals(testCoupon.getCode(), result.get(0).getCode());
    }

    @Test
    void getCouponByCode_ExistingCoupon_ReturnsCoupon() {
        // Given
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));

        // When
        Coupon result = couponService.getCouponByCode("TEST20OFF");

        // Then
        assertEquals(testCoupon.getCode(), result.getCode());
        assertEquals(testCoupon.getDiscountType(), result.getDiscountType());
    }

    @Test
    void getCouponByCode_NonExistingCoupon_ThrowsException() {
        // Given
        when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponService.getCouponByCode("INVALID"));
        assertTrue(exception.getMessage().contains("Coupon not found with code: INVALID"));
    }

    @Test
    void validateCoupon_ValidUnusedCoupon_ReturnsTrue() {
        // Given
        testCoupon.setUsed(false);
        testCoupon.setExpirationDate(LocalDateTime.now().plusDays(10));
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));

        // When
        boolean result = couponService.validateCoupon("TEST20OFF");

        // Then
        assertTrue(result);
    }

    @Test
    void validateCoupon_UsedCoupon_ReturnsFalse() {
        // Given
        testCoupon.setUsed(true);
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));

        // When
        boolean result = couponService.validateCoupon("TEST20OFF");

        // Then
        assertFalse(result);
    }

    @Test
    void validateCoupon_ExpiredCoupon_ReturnsFalse() {
        // Given
        testCoupon.setUsed(false);
        testCoupon.setExpirationDate(LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));

        // When
        boolean result = couponService.validateCoupon("TEST20OFF");

        // Then
        assertFalse(result);
    }

    @Test
    void generateCouponForPoints_SufficientPoints_CreatesCoupon() {
        // Given
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);
        when(pointTransactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(new PointTransaction());

        // When
        Coupon result = couponService.generateCouponForPoints(
                userId, DiscountType.PERCENTAGE, new BigDecimal("10"), 300,
                new BigDecimal("50"), new BigDecimal("25"),
                LocalDateTime.now().plusDays(30), 1);

        // Then
        assertNotNull(result);
        verify(pointTransactionService).recordTransactionWithIdempotency(
                eq(userId), eq(TransactionType.REDEEM), eq(300), any(), any());
        verify(couponRepository).save(any(Coupon.class));
        verify(kafkaService).publishCouponGenerated(any(Coupon.class), eq("POINTS_PURCHASE"));
    }

    @Test
    void generateCouponForPoints_InsufficientPoints_ThrowsException() {
        // Given
        testCrm.setTotalPoints(100); // Less than required 300 points
        when(crmService.getByUserId(userId)).thenReturn(testCrm);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponService.generateCouponForPoints(
                        userId, DiscountType.PERCENTAGE, new BigDecimal("10"), 300,
                        new BigDecimal("50"), new BigDecimal("25"),
                        LocalDateTime.now().plusDays(30), 1));

        assertTrue(exception.getMessage().contains("Insufficient points"));
        verify(pointTransactionService, never()).recordTransactionWithIdempotency(any(), any(), anyInt(), any(), any());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void generateCouponFromPackage_ValidPackage_CreatesCoupon() {
        // Given
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);
        when(pointTransactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(new PointTransaction());

        // When
        Coupon result = couponService.generateCouponFromPackage(
                userId, CouponService.CouponPackage.BASIC_5_PERCENT);

        // Then
        assertNotNull(result);
        verify(pointTransactionService).recordTransactionWithIdempotency(
                eq(userId), eq(TransactionType.REDEEM), eq(150), any(), any()); // BASIC_5_PERCENT costs 150 points
        verify(couponRepository).save(any(Coupon.class));
        verify(kafkaService).publishCouponGenerated(any(Coupon.class), eq("POINTS_PURCHASE"));
    }

    @Test
    void getAvailableCouponPackages_ReturnsAffordablePackages() {
        // Given
        testCrm.setTotalPoints(200); // Can afford BASIC_5_PERCENT (150) but not STANDARD_10_PERCENT (300)
        when(crmService.getByUserId(userId)).thenReturn(testCrm);

        // When
        List<CouponService.CouponPackage> result = couponService.getAvailableCouponPackages(userId);

        // Then
        assertTrue(result.contains(CouponService.CouponPackage.BASIC_5_PERCENT));
        assertTrue(result.contains(CouponService.CouponPackage.FIXED_5_DOLLAR)); // costs 100
        assertFalse(result.contains(CouponService.CouponPackage.STANDARD_10_PERCENT)); // costs 300
    }

    @Test
    void validateCoupon_ValidCouponWithSufficientAmount_ReturnsTrue() {
        // Given
        testCoupon.setMinPurchaseAmount(new BigDecimal("50"));
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));

        // When
        boolean result = couponService.validateCoupon("TEST20OFF", new BigDecimal("75"));

        // Then
        assertTrue(result);
        verify(kafkaService).publishCouponValidated(
                eq("TEST20OFF"), eq(userId), eq(true), eq(new BigDecimal("75")), any());
    }

    @Test
    void validateCoupon_ValidCouponWithInsufficientAmount_ReturnsFalse() {
        // Given
        testCoupon.setMinPurchaseAmount(new BigDecimal("50"));
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));

        // When
        boolean result = couponService.validateCoupon("TEST20OFF", new BigDecimal("25"));

        // Then
        assertFalse(result);
        verify(kafkaService).publishCouponValidated(
                eq("TEST20OFF"), eq(userId), eq(false), eq(new BigDecimal("25")), any());
    }

    @Test
    void applyCoupon_PercentageDiscount_CalculatesCorrectDiscount() {
        // Given
        testCoupon.setDiscountType(DiscountType.PERCENTAGE);
        testCoupon.setDiscountValue(new BigDecimal("20"));
        testCoupon.setMinPurchaseAmount(new BigDecimal("50"));
        testCoupon.setMaxDiscountAmount(new BigDecimal("50"));
        testCoupon.setUsed(false);
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);

        BigDecimal purchaseAmount = new BigDecimal("100");

        // When
        BigDecimal result = couponService.applyCoupon("TEST20OFF", purchaseAmount);

        // Then
        assertEquals(new BigDecimal("80"), result); // 100 - (100 * 0.20) = 80
        verify(couponRepository).save(argThat(coupon -> coupon.isUsed()));
        verify(kafkaService).publishCouponRedeemed(any(), eq(purchaseAmount), eq(new BigDecimal("20")), eq(result), isNull());
    }

    @Test
    void applyCoupon_FixedAmountDiscount_CalculatesCorrectDiscount() {
        // Given
        testCoupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        testCoupon.setDiscountValue(new BigDecimal("15"));
        testCoupon.setMinPurchaseAmount(new BigDecimal("50"));
        testCoupon.setUsed(false);
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);

        BigDecimal purchaseAmount = new BigDecimal("100");

        // When
        BigDecimal result = couponService.applyCoupon("TEST20OFF", purchaseAmount);

        // Then
        assertEquals(new BigDecimal("85"), result); // 100 - 15 = 85
        verify(couponRepository).save(argThat(coupon -> coupon.isUsed()));
        verify(kafkaService).publishCouponRedeemed(any(), eq(purchaseAmount), eq(new BigDecimal("15")), eq(result), isNull());
    }

    @Test
    void applyCoupon_PercentageWithMaxDiscount_CapsAtMaximum() {
        // Given
        testCoupon.setDiscountType(DiscountType.PERCENTAGE);
        testCoupon.setDiscountValue(new BigDecimal("50")); // 50% discount
        testCoupon.setMinPurchaseAmount(new BigDecimal("50"));
        testCoupon.setMaxDiscountAmount(new BigDecimal("30")); // Cap at $30
        testCoupon.setUsed(false);
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);

        BigDecimal purchaseAmount = new BigDecimal("100");

        // When
        BigDecimal result = couponService.applyCoupon("TEST20OFF", purchaseAmount);

        // Then
        assertEquals(new BigDecimal("70"), result); // 100 - 30 (capped) = 70
        verify(kafkaService).publishCouponRedeemed(any(), eq(purchaseAmount), eq(new BigDecimal("30")), eq(result), isNull());
    }

    @Test
    void applyCoupon_InvalidCoupon_ThrowsException() {
        // Given
        testCoupon.setUsed(true); // Already used
        when(couponRepository.findByCode("TEST20OFF")).thenReturn(Optional.of(testCoupon));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> couponService.applyCoupon("TEST20OFF", new BigDecimal("100")));
        assertTrue(exception.getMessage().contains("Invalid coupon"));
    }

    @Test
    void getUserActiveCoupons_ReturnsUnusedCoupons() {
        // Given
        List<Coupon> activeCoupons = Arrays.asList(testCoupon);
        when(couponRepository.findByUserIdAndIsUsedFalse(userId)).thenReturn(activeCoupons);

        // When
        List<Coupon> result = couponService.getUserActiveCoupons(userId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testCoupon.getCode(), result.get(0).getCode());
        assertFalse(result.get(0).isUsed());
    }

    @Test
    void generateCouponCode_GeneratesValidCode() {
        // When
        String result = couponService.generateCouponCode();

        // Then
        assertNotNull(result);
        assertEquals(8, result.length());
        assertTrue(result.matches("[A-Z0-9]+"));
    }

    @Test
    void generatePromotionalCoupon_CreatesCoupon() {
        // Given
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);

        // When
        Coupon result = couponService.generateCoupon(
                userId, DiscountType.PERCENTAGE, new BigDecimal("10"),
                new BigDecimal("50"), new BigDecimal("25"),
                LocalDateTime.now().plusDays(30), 1);

        // Then
        assertNotNull(result);
        verify(couponRepository).save(any(Coupon.class));
        verify(kafkaService).publishCouponGenerated(any(Coupon.class), eq("PROMOTIONAL"));
        // Should not deduct points for promotional coupons
        verify(pointTransactionService, never()).recordTransactionWithIdempotency(any(), any(), anyInt(), any(), any());
    }

    @Test
    void generateRewardCoupon_ValidRequest_CreatesCoupon() {
        // Given
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);
        when(pointTransactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(new PointTransaction());

        // When
        Coupon result = couponService.generateRewardCoupon(
                userId, DiscountType.PERCENTAGE, new BigDecimal("15"), 400);

        // Then
        assertNotNull(result);
        verify(pointTransactionService).recordTransactionWithIdempotency(
                eq(userId), eq(TransactionType.REDEEM), eq(400), any(), any());
        verify(couponRepository).save(any(Coupon.class));
        verify(kafkaService).publishCouponGenerated(any(Coupon.class), eq("POINTS_PURCHASE"));
    }

    @Test
    void generateCouponForPoints_OptimisticLockingFailure_RetriesSuccessfully() {
        // Given
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(couponRepository.save(any(Coupon.class)))
                .thenThrow(new OptimisticLockingFailureException("Retry test"))
                .thenReturn(testCoupon); // Succeed on retry
        when(pointTransactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(new PointTransaction());

        // When
        Coupon result = couponService.generateCouponForPoints(
                userId, DiscountType.PERCENTAGE, new BigDecimal("10"), 300,
                new BigDecimal("50"), new BigDecimal("25"),
                LocalDateTime.now().plusDays(30), 1);

        // Then
        assertNotNull(result);
        verify(couponRepository, times(2)).save(any(Coupon.class)); // Called twice due to retry
        verify(kafkaService).publishCouponGenerated(any(Coupon.class), eq("POINTS_PURCHASE"));
    }
}