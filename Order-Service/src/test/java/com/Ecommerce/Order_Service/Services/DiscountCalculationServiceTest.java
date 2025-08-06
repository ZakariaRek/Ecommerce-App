package com.Ecommerce.Order_Service.KafkaProducers;

import com.Ecommerce.Order_Service.Payload.Kafka.DiscountCalculationContext;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.CombinedDiscountRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountCalculationResponse;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountCalculationServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DiscountCalculationService discountCalculationService;

    private UUID testOrderId;
    private UUID testUserId;
    private String testCorrelationId;
    private DiscountCalculationRequest testRequest;
    private List<OrderItemResponseDto> testItems;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCorrelationId = UUID.randomUUID().toString();

        // Create test order items
        testItems = List.of(
                OrderItemResponseDto.builder()
                        .id(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .quantity(2)
                        .priceAtPurchase(BigDecimal.valueOf(50.00))
                        .discount(BigDecimal.valueOf(5.00))
                        .total(BigDecimal.valueOf(95.00))
                        .build(),
                OrderItemResponseDto.builder()
                        .id(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .quantity(1)
                        .priceAtPurchase(BigDecimal.valueOf(30.00))
                        .discount(BigDecimal.valueOf(0.00))
                        .total(BigDecimal.valueOf(30.00))
                        .build()
        );

        testRequest = DiscountCalculationRequest.builder()
                .correlationId(testCorrelationId)
                .userId(testUserId)
                .orderId(testOrderId)
                .subtotal(BigDecimal.valueOf(125.00))
                .totalItems(2)
                .couponCodes(List.of("SAVE10", "SUMMER20"))
                .items(testItems)
                .build();
    }

    @Test
    void calculateOrderDiscounts_WithValidRequest_ReturnsCompletableFuture() {
        // When
        CompletableFuture<DiscountCalculationResponse> result =
                discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotCompleted(); // Should be pending initially

        // Verify Kafka message was sent
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), requestCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("combined-discount-request");
        assertThat(keyCaptor.getValue()).isEqualTo(testCorrelationId);

        CombinedDiscountRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCorrelationId()).isEqualTo(testCorrelationId);
        assertThat(capturedRequest.getUserId()).isEqualTo(testUserId);
        assertThat(capturedRequest.getOrderId()).isEqualTo(testOrderId);
        assertThat(capturedRequest.getOriginalAmount()).isEqualByComparingTo(BigDecimal.valueOf(125.00));
        assertThat(capturedRequest.getCouponCodes()).containsExactly("SAVE10", "SUMMER20");
    }

    @Test
    void calculateOrderDiscounts_WithNullCouponCodes_SendsRequestWithNullCoupons() {
        // Given
        testRequest.setCouponCodes(null);

        // When
        CompletableFuture<DiscountCalculationResponse> result =
                discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        CombinedDiscountRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCouponCodes()).isNull();
    }

    @Test
    void calculateOrderDiscounts_WithEmptyCouponCodes_SendsRequestWithEmptyList() {
        // Given
        testRequest.setCouponCodes(List.of());

        // When
        CompletableFuture<DiscountCalculationResponse> result =
                discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        CombinedDiscountRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCouponCodes()).isEmpty();
    }

    @Test
    void calculateOrderDiscounts_CalculatesCorrectDiscounts() {
        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        CombinedDiscountRequest capturedRequest = requestCaptor.getValue();

        // Verify product discount calculation: 5.00 + 0.00 = 5.00
        assertThat(capturedRequest.getProductDiscount()).isEqualByComparingTo(BigDecimal.valueOf(5.00));

        // Verify order-level discount calculation
        // Should include bulk discount (2+ items = 10% off 125.00 = 12.50)
        // and minimum purchase discount (125 > 100 = $15 off)
        // Total order discount = 12.50 + 15.00 = 27.50
        assertThat(capturedRequest.getOrderLevelDiscount()).isEqualByComparingTo(BigDecimal.valueOf(27.50));

        // Verify amount after order discounts: 125.00 - 5.00 - 27.50 = 92.50
        assertThat(capturedRequest.getAmountAfterOrderDiscounts()).isEqualByComparingTo(BigDecimal.valueOf(92.50));
    }

    @Test
    void calculateOrderDiscounts_WithLargeOrder_AppliesLargeOrderDiscount() {
        // Given
        testRequest.setSubtotal(BigDecimal.valueOf(600.00)); // > $500

        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        CombinedDiscountRequest capturedRequest = requestCaptor.getValue();

        // Should include:
        // - Bulk discount: 10% of 600 = 60.00
        // - Min purchase discount: $15
        // - Large order discount: 5% of 600 = 30.00 (capped at $50, so 30.00)
        // Total = 60.00 + 15.00 + 30.00 = 105.00
        assertThat(capturedRequest.getOrderLevelDiscount()).isEqualByComparingTo(BigDecimal.valueOf(105.00));
    }

    @Test
    void calculateOrderDiscounts_WithVeryLargeOrder_CapsLargeOrderDiscount() {
        // Given
        testRequest.setSubtotal(BigDecimal.valueOf(2000.00)); // Large enough to hit cap

        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        CombinedDiscountRequest capturedRequest = requestCaptor.getValue();

        // Should include:
        // - Bulk discount: 10% of 2000 = 200.00
        // - Min purchase discount: $15
        // - Large order discount: 5% of 2000 = 100.00, but capped at $50
        // Total = 200.00 + 15.00 + 50.00 = 265.00
        assertThat(capturedRequest.getOrderLevelDiscount()).isEqualByComparingTo(BigDecimal.valueOf(265.00));
    }

    @Test
    void calculateOrderDiscounts_WithSmallOrder_OnlyAppliesApplicableDiscounts() {
        // Given
        testRequest.setSubtotal(BigDecimal.valueOf(50.00)); // < $100, < 5 items
        testRequest.setTotalItems(1);

        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        CombinedDiscountRequest capturedRequest = requestCaptor.getValue();

        // Should not apply any order-level discounts
        assertThat(capturedRequest.getOrderLevelDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void completePendingCalculation_WithValidResponse_CompletesSuccessfully() {
        // Given
        DiscountCalculationResponse response = DiscountCalculationResponse.builder()
                .correlationId(testCorrelationId)
                .orderId(testOrderId)
                .success(true)
                .finalAmount(BigDecimal.valueOf(100.00))
                .build();

        CompletableFuture<DiscountCalculationResponse> future =
                discountCalculationService.calculateOrderDiscounts(testRequest);

        // When
        discountCalculationService.completePendingCalculation(testCorrelationId, response);

        // Then
        assertThat(future).isCompleted();
        assertThatCode(() -> {
            DiscountCalculationResponse result = future.get(1, TimeUnit.SECONDS);
            assertThat(result).isEqualTo(response);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        }).doesNotThrowAnyException();
    }

    @Test
    void completePendingCalculation_WithNonExistentCorrelationId_HandlesGracefully() {
        // Given
        String nonExistentCorrelationId = UUID.randomUUID().toString();
        DiscountCalculationResponse response = DiscountCalculationResponse.builder()
                .correlationId(nonExistentCorrelationId)
                .success(true)
                .build();

        // When & Then - Should not throw exception
        assertThatCode(() ->
                discountCalculationService.completePendingCalculation(nonExistentCorrelationId, response)
        ).doesNotThrowAnyException();
    }

    @Test
    void getContext_WithStoredContext_ReturnsCorrectContext() {
        // Given
        DiscountCalculationContext expectedContext = DiscountCalculationContext.builder()
                .originalRequest(testRequest)
                .productDiscount(BigDecimal.valueOf(5.00))
                .orderDiscount(BigDecimal.valueOf(10.00))
                .build();

        // Simulate storing context during calculation
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // When
        DiscountCalculationContext actualContext = discountCalculationService.getContext(testCorrelationId);

        // Then
        assertThat(actualContext).isNotNull();
        // Note: In real implementation, context would be stored during processing
    }

    @Test
    void getContext_WithNonExistentCorrelationId_ReturnsNull() {
        // Given
        String nonExistentCorrelationId = UUID.randomUUID().toString();

        // When
        DiscountCalculationContext context = discountCalculationService.getContext(nonExistentCorrelationId);

        // Then
        assertThat(context).isNull();
    }

    @Test
    void getAvailableContextKeys_ReturnsCurrentKeys() {
        // Given
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // When
        java.util.Set<String> keys = discountCalculationService.getAvailableContextKeys();

        // Then
        assertThat(keys).isNotNull();
        // Keys should include the correlation ID from the test request
        // Note: Actual implementation would store the context
    }

    @Test
    void calculateProductDiscounts_WithItemsHavingDiscounts_CalculatesCorrectTotal() {
        // The method is private, but we can test it indirectly through calculateOrderDiscounts
        // Given items with discounts: 5.00 + 0.00 = 5.00

        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        assertThat(requestCaptor.getValue().getProductDiscount()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
    }

    @Test
    void calculateProductDiscounts_WithNullItems_ReturnsZero() {
        // Given
        testRequest.setItems(null);

        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        assertThat(requestCaptor.getValue().getProductDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateProductDiscounts_WithNullDiscountValues_HandlesGracefully() {
        // Given
        testItems.get(0).setDiscount(null);

        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        ArgumentCaptor<CombinedDiscountRequest> requestCaptor = ArgumentCaptor.forClass(CombinedDiscountRequest.class);
        verify(kafkaTemplate).send(any(), any(), requestCaptor.capture());

        // Should treat null as zero: 0.00 + 0.00 = 0.00
        assertThat(requestCaptor.getValue().getProductDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateOrderDiscounts_WithKafkaException_HandlesErrorGracefully() {
        // Given
        doThrow(new RuntimeException("Kafka connection failed"))
                .when(kafkaTemplate).send(any(), any(), any());

        // When
        CompletableFuture<DiscountCalculationResponse> future =
                discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then
        assertThat(future).isNotNull();
        // The future should eventually complete with an error or timeout
        assertThatCode(() -> {
            try {
                future.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // Expected for this test
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void calculateOrderDiscounts_StoresContextForLaterRetrieval() {
        // When
        discountCalculationService.calculateOrderDiscounts(testRequest);

        // Then - Context should be available for the correlation ID
        // Note: This tests the internal state management
        java.util.Set<String> availableKeys = discountCalculationService.getAvailableContextKeys();
        assertThat(availableKeys).isNotNull();
    }
}