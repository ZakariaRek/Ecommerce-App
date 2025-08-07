package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Config.TierThresholdConfig;
import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CRMKafkaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CRMServiceTest {

    @Mock
    private CRMRepository crmRepository;

    @Mock
    private CRMKafkaService kafkaService;

    @Mock
    private TierThresholdConfig tierConfig;

    @InjectMocks
    private CRMService crmService;

    private UUID userId;
    private CRM testCrm;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testCrm = new CRM();
        testCrm.setId(UUID.randomUUID());
        testCrm.setUserId(userId);
        testCrm.setTotalPoints(500);
        testCrm.setMembershipLevel(MembershipTier.SILVER);
        testCrm.setJoinDate(LocalDateTime.now().minusMonths(6));
        testCrm.setLastActivity(LocalDateTime.now().minusDays(5));

        // Setup tier thresholds
        when(tierConfig.getBronzeThreshold()).thenReturn(0);
        when(tierConfig.getSilverThreshold()).thenReturn(500);
        when(tierConfig.getGoldThreshold()).thenReturn(2000);
        when(tierConfig.getPlatinumThreshold()).thenReturn(5000);
        when(tierConfig.getDiamondThreshold()).thenReturn(10000);
    }

    @Test
    void getByUserId_ExistingUser_ReturnsUser() {
        // Given
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        CRM result = crmService.getByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(500, result.getTotalPoints());
        assertEquals(MembershipTier.SILVER, result.getMembershipLevel());
    }

    @Test
    void getByUserId_NonExistingUser_ThrowsException() {
        // Given
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> crmService.getByUserId(userId));
        assertTrue(exception.getMessage().contains("User not found in loyalty system"));
    }

    @Test
    void findByUserId_ExistingUser_ReturnsOptionalUser() {
        // Given
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        Optional<CRM> result = crmService.findByUserId(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    void isUserInLoyaltyProgram_ExistingUser_ReturnsTrue() {
        // Given
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        boolean result = crmService.isUserInLoyaltyProgram(userId);

        // Then
        assertTrue(result);
    }

    @Test
    void isUserInLoyaltyProgram_NonExistingUser_ReturnsFalse() {
        // Given
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        boolean result = crmService.isUserInLoyaltyProgram(userId);

        // Then
        assertFalse(result);
    }

    @Test
    void earnPoints_ValidUser_UpdatesPointsAndTier() {
        // Given
        testCrm.setTotalPoints(1800); // Close to Gold tier (2000)
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(crmRepository.save(any(CRM.class))).thenReturn(testCrm);

        // When
        crmService.earnPoints(userId, 300, "Test Order");

        // Then
        verify(crmRepository).save(argThat(crm -> {
            assertEquals(2100, crm.getTotalPoints()); // 1800 + 300
            assertEquals(MembershipTier.GOLD, crm.getMembershipLevel()); // Should upgrade to Gold
            return true;
        }));

        verify(kafkaService).publishPointsEarned(
                eq(userId), eq(300), eq(2100), eq("Test Order"), isNull(), eq(MembershipTier.GOLD));

        verify(kafkaService).publishMembershipTierChanged(
                eq(userId), eq(MembershipTier.SILVER), eq(MembershipTier.GOLD),
                eq(2100), eq("POINTS_INCREASE"));
    }

    @Test
    void earnPoints_NoTierUpgrade_UpdatesPointsOnly() {
        // Given
        testCrm.setTotalPoints(800); // Stays in Silver tier
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(crmRepository.save(any(CRM.class))).thenReturn(testCrm);

        // When
        crmService.earnPoints(userId, 100, "Test Order");

        // Then
        verify(crmRepository).save(argThat(crm -> {
            assertEquals(900, crm.getTotalPoints());
            assertEquals(MembershipTier.SILVER, crm.getMembershipLevel()); // No tier change
            return true;
        }));

        verify(kafkaService).publishPointsEarned(
                eq(userId), eq(100), eq(900), eq("Test Order"), isNull(), eq(MembershipTier.SILVER));

        // No tier change event should be published
        verify(kafkaService, never()).publishMembershipTierChanged(any(), any(), any(), anyInt(), any());
    }

    @Test
    void redeemPoints_SufficientPoints_DeductsPoints() {
        // Given
        testCrm.setTotalPoints(1000);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(crmRepository.save(any(CRM.class))).thenReturn(testCrm);

        // When
        crmService.redeemPoints(userId, 200);

        // Then
        verify(crmRepository).save(argThat(crm -> {
            assertEquals(800, crm.getTotalPoints());
            return true;
        }));

        verify(kafkaService).publishPointsRedeemed(
                eq(userId), eq(200), eq(800), eq("POINTS_REDEMPTION"),
                isNull(), eq(MembershipTier.SILVER));
    }

    @Test
    void redeemPoints_InsufficientPoints_ThrowsException() {
        // Given
        testCrm.setTotalPoints(100);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> crmService.redeemPoints(userId, 200));
        assertTrue(exception.getMessage().contains("Insufficient points balance"));
    }

    @Test
    void getPointsNeededForNextTier_BronzeUser_ReturnsCorrectAmount() {
        // Given
        testCrm.setTotalPoints(300);
        testCrm.setMembershipLevel(MembershipTier.BRONZE);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        int pointsNeeded = crmService.getPointsNeededForNextTier(userId);

        // Then
        assertEquals(200, pointsNeeded); // 500 (Silver threshold) - 300 (current points)
    }

    @Test
    void getPointsNeededForNextTier_DiamondUser_ReturnsZero() {
        // Given
        testCrm.setTotalPoints(15000);
        testCrm.setMembershipLevel(MembershipTier.DIAMOND);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        int pointsNeeded = crmService.getPointsNeededForNextTier(userId);

        // Then
        assertEquals(0, pointsNeeded); // Already at highest tier
    }

    @Test
    void getNextTier_BronzeUser_ReturnsSilver() {
        // Given
        testCrm.setMembershipLevel(MembershipTier.BRONZE);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        MembershipTier nextTier = crmService.getNextTier(userId);

        // Then
        assertEquals(MembershipTier.SILVER, nextTier);
    }

    @Test
    void getNextTier_DiamondUser_ReturnsDiamond() {
        // Given
        testCrm.setMembershipLevel(MembershipTier.DIAMOND);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        MembershipTier nextTier = crmService.getNextTier(userId);

        // Then
        assertEquals(MembershipTier.DIAMOND, nextTier); // Already at highest
    }

    @Test
    void calculateLoyaltyScore_ReturnsValidScore() {
        // Given
        testCrm.setTotalPoints(1000);
        testCrm.setJoinDate(LocalDateTime.now().minusDays(100));
        testCrm.setLastActivity(LocalDateTime.now().minusDays(10)); // Recent activity
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        double score = crmService.calculateLoyaltyScore(userId);

        // Then
        assertTrue(score > 0);
        assertTrue(score < 1000); // Should be reasonable
    }

    @Test
    void adjustPoints_PositiveAdjustment_IncreasesPoints() {
        // Given
        testCrm.setTotalPoints(800);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(crmRepository.save(any(CRM.class))).thenReturn(testCrm);

        // When
        crmService.adjustPoints(userId, 500, "Admin bonus", "admin123");

        // Then
        verify(crmRepository).save(argThat(crm -> {
            assertEquals(1300, crm.getTotalPoints());
            return true;
        }));

        verify(kafkaService).publishPointsAdjusted(
                eq(userId), eq(500), eq(1300), eq("Admin bonus"),
                eq("admin123"), eq(MembershipTier.SILVER));
    }

    @Test
    void adjustPoints_NegativeAdjustment_DecreasesPoints() {
        // Given
        testCrm.setTotalPoints(1000);
        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(crmRepository.save(any(CRM.class))).thenReturn(testCrm);

        // When
        crmService.adjustPoints(userId, -300, "Point correction", "admin123");

        // Then
        verify(crmRepository).save(argThat(crm -> {
            assertEquals(700, crm.getTotalPoints());
            return true;
        }));

        verify(kafkaService).publishPointsAdjusted(
                eq(userId), eq(-300), eq(700), eq("Point correction"),
                eq("admin123"), eq(MembershipTier.SILVER));
    }
}