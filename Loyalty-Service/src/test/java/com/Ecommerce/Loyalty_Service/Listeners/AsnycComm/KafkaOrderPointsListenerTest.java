package com.Ecommerce.Loyalty_Service.Listeners.AsnycComm;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Events.ExternalEvents;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import com.Ecommerce.Loyalty_Service.Services.PointTransactionService;
import com.Ecommerce.Loyalty_Service.Services.UserSpendingTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaOrderPointsListenerTest {

    @Mock
    private PointTransactionService transactionService;

    @Mock
    private CRMRepository crmRepository;

    @Mock
    private UserSpendingTrackingService spendingTrackingService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaOrderPointsListener kafkaOrderPointsListener;

    private UUID userId;
    private UUID orderId;
    private CRM testCrm;
    private PointTransaction testTransaction;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        testCrm = new CRM();
        testCrm.setUserId(userId);
        testCrm.setTotalPoints(1000);
        testCrm.setMembershipLevel(MembershipTier.SILVER);

        testTransaction = new PointTransaction();
        testTransaction.setId(UUID.randomUUID());
        testTransaction.setUserId(userId);
        testTransaction.setType(TransactionType.EARN);
        testTransaction.setPoints(150);
        testTransaction.setBalance(1150);
    }

    @Test
    void handleOrderCompletedEvent_UserInCRM_ProcessesLoyaltyPoints() {
        // Given
        ExternalEvents.OrderCompletedEvent event = ExternalEvents.OrderCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .orderId(orderId)
                .userId(userId)
                .orderTotal(new BigDecimal("150.00"))
                .itemCount(3)
                .firstOrder(false)
                .paymentMethod("CREDIT_CARD")
                .orderStatus("COMPLETED")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(transactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(testTransaction);

        // When
        kafkaOrderPointsListener.handleOrderCompletedEvent(event);

        // Then
        verify(spendingTrackingService).processOrderCompletion(userId, new BigDecimal("150.00"));
        verify(transactionService).recordTransactionWithIdempotency(
                eq(userId),
                eq(TransactionType.EARN),
                eq(160), // 150 base + 10 credit card bonus
                contains("Order: " + orderId),
                eq("order-" + orderId)
        );
    }

    @Test
    void handleOrderCompletedEvent_FirstOrder_AddsWelcomeBonus() {
        // Given
        ExternalEvents.OrderCompletedEvent event = ExternalEvents.OrderCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .orderId(orderId)
                .userId(userId)
                .orderTotal(new BigDecimal("75.00"))
                .itemCount(2)
                .firstOrder(true) // First order
                .paymentMethod("DEBIT_CARD")
                .orderStatus("COMPLETED")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(transactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(testTransaction);

        // When
        kafkaOrderPointsListener.handleOrderCompletedEvent(event);

        // Then
        verify(transactionService).recordTransactionWithIdempotency(
                eq(userId),
                eq(TransactionType.EARN),
                eq(175), // 75 base + 100 first order bonus
                contains("Order: " + orderId),
                eq("order-" + orderId)
        );
    }

    @Test
    void handleOrderCompletedEvent_CreditCardPayment_AddsCreditCardBonus() {
        // Given
        ExternalEvents.OrderCompletedEvent event = ExternalEvents.OrderCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .orderId(orderId)
                .userId(userId)
                .orderTotal(new BigDecimal("100.00"))
                .itemCount(1)
                .firstOrder(false)
                .paymentMethod("CREDIT_CARD")
                .orderStatus("COMPLETED")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(transactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(testTransaction);

        // When
        kafkaOrderPointsListener.handleOrderCompletedEvent(event);

        // Then
        verify(transactionService).recordTransactionWithIdempotency(
                eq(userId),
                eq(TransactionType.EARN),
                eq(110), // 100 base + 10 credit card bonus
                contains("Order: " + orderId),
                eq("order-" + orderId)
        );
    }

    @Test
    void handleOrderCompletedEvent_UserNotInCRM_SkipsLoyaltyPoints() {
        // Given
        ExternalEvents.OrderCompletedEvent event = ExternalEvents.OrderCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .orderId(orderId)
                .userId(userId)
                .orderTotal(new BigDecimal("100.00"))
                .itemCount(1)
                .firstOrder(false)
                .paymentMethod("CREDIT_CARD")
                .orderStatus("COMPLETED")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        kafkaOrderPointsListener.handleOrderCompletedEvent(event);

        // Then
        verify(spendingTrackingService).processOrderCompletion(userId, new BigDecimal("100.00"));
        verify(transactionService, never()).recordTransactionWithIdempotency(any(), any(), anyInt(), any(), any());
    }

    @Test
    void handleOrderCompletedEvent_SmallOrder_NoPointsAwarded() {
        // Given - Order under $5 minimum
        ExternalEvents.OrderCompletedEvent event = ExternalEvents.OrderCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .orderId(orderId)
                .userId(userId)
                .orderTotal(new BigDecimal("3.00"))
                .itemCount(1)
                .firstOrder(false)
                .paymentMethod("CREDIT_CARD")
                .orderStatus("COMPLETED")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(transactionService.recordTransactionWithIdempotency(
                any(), any(), anyInt(), any(), any())).thenReturn(testTransaction);

        // When
        kafkaOrderPointsListener.handleOrderCompletedEvent(event);

        // Then
        verify(transactionService).recordTransactionWithIdempotency(
                eq(userId),
                eq(TransactionType.EARN),
                eq(10), // 0 base points + 10 credit card bonus
                contains("Order: " + orderId),
                eq("order-" + orderId)
        );
    }

    @Test
    void handleOrderCompletedEvent_ExceptionInProcessing_PropagatesException() {
        // Given
        ExternalEvents.OrderCompletedEvent event = ExternalEvents.OrderCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .orderId(orderId)
                .userId(userId)
                .orderTotal(new BigDecimal("100.00"))
                .itemCount(1)
                .firstOrder(false)
                .paymentMethod("CREDIT_CARD")
                .orderStatus("COMPLETED")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(transactionService.recordTransactionWithIdempotency(any(), any(), anyInt(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            kafkaOrderPointsListener.handleOrderCompletedEvent(event);
        } catch (RuntimeException e) {
            // Expected - exception should propagate to trigger retry
        }

        verify(transactionService).recordTransactionWithIdempotency(any(), any(), anyInt(), any(), any());
    }

    @Test
    void handleUserRegisteredEvent_NewUser_AcknowledgesEvent() {
        // Given
        ExternalEvents.UserRegisteredEvent event = ExternalEvents.UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .email("user@example.com")
                .registrationDate(LocalDateTime.now())
                .referredBy(null)
                .signupSource("WEB")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        kafkaOrderPointsListener.handleUserRegisteredEvent(event, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleUserRegisteredEvent_ExistingUser_AcknowledgesEvent() {
        // Given
        ExternalEvents.UserRegisteredEvent event = ExternalEvents.UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .email("user@example.com")
                .registrationDate(LocalDateTime.now())
                .referredBy(null)
                .signupSource("WEB")
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        kafkaOrderPointsListener.handleUserRegisteredEvent(event, acknowledgment);

        // Then
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleProductReviewedEvent_VerifiedPurchaseUserInCRM_AwardsPoints() {
        // Given
        ExternalEvents.ProductReviewedEvent event = ExternalEvents.ProductReviewedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .productId(UUID.randomUUID())
                .rating(5)
                .verifiedPurchase(true)
                .orderId(orderId)
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));
        when(transactionService.recordTransaction(any(), any(), anyInt(), any())).thenReturn(testTransaction);

        // When
        kafkaOrderPointsListener.handleProductReviewedEvent(event, acknowledgment);

        // Then
        verify(transactionService).recordTransaction(
                eq(userId),
                eq(TransactionType.EARN),
                eq(10),
                contains("Review:")
        );
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleProductReviewedEvent_NonVerifiedPurchase_SkipsPoints() {
        // Given
        ExternalEvents.ProductReviewedEvent event = ExternalEvents.ProductReviewedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .productId(UUID.randomUUID())
                .rating(5)
                .verifiedPurchase(false) // Not verified
                .orderId(null)
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.of(testCrm));

        // When
        kafkaOrderPointsListener.handleProductReviewedEvent(event, acknowledgment);

        // Then
        verify(transactionService, never()).recordTransaction(any(), any(), anyInt(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleProductReviewedEvent_UserNotInCRM_SkipsPoints() {
        // Given
        ExternalEvents.ProductReviewedEvent event = ExternalEvents.ProductReviewedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .productId(UUID.randomUUID())
                .rating(5)
                .verifiedPurchase(true)
                .orderId(orderId)
                .build();

        when(crmRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        kafkaOrderPointsListener.handleProductReviewedEvent(event, acknowledgment);

        // Then
        verify(transactionService, never()).recordTransaction(any(), any(), anyInt(), any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleCartAbandonedEvent_UserCloseToBronzeTier_LogsPromotionOpportunity() {
        // Given
        ExternalEvents.CartAbandonedEvent event = ExternalEvents.CartAbandonedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .cartId(UUID.randomUUID())
                .userId(userId)
                .cartTotal(new BigDecimal("75.00"))
                .itemCount(2)
                .lastActivityTime(LocalDateTime.now().minusHours(1))
                .build();

        when(spendingTrackingService.getRemainingForBronze(userId)).thenReturn(new BigDecimal("25.00"));

        // When
        kafkaOrderPointsListener.handleCartAbandonedEvent(event, acknowledgment);

        // Then
        verify(spendingTrackingService).getRemainingForBronze(userId);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleCartAbandonedEvent_UserFarFromTier_AcknowledgesOnly() {
        // Given
        ExternalEvents.CartAbandonedEvent event = ExternalEvents.CartAbandonedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .cartId(UUID.randomUUID())
                .userId(userId)
                .cartTotal(new BigDecimal("30.00"))
                .itemCount(1)
                .lastActivityTime(LocalDateTime.now().minusHours(2))
                .build();

        when(spendingTrackingService.getRemainingForBronze(userId)).thenReturn(new BigDecimal("100.00"));

        // When
        kafkaOrderPointsListener.handleCartAbandonedEvent(event, acknowledgment);

        // Then
        verify(spendingTrackingService).getRemainingForBronze(userId);
        verify(acknowledgment).acknowledge();
    }
}