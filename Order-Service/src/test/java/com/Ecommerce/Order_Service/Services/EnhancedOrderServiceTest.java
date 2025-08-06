package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.DiscountApplication;
import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.KafkaProducers.DiscountCalculationService;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountBreakdown;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountCalculationResponse;
import com.Ecommerce.Order_Service.Repositories.DiscountApplicationRepository;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnhancedOrderServiceTest {

    @Mock
    private DiscountCalculationService discountCalculationService;

    @Mock
    private DiscountApplicationRepository discountApplicationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private EnhancedOrderService enhancedOrderService;

    private UUID testOrderId;
    private UUID testUserId;
    private UUID testCartId;
    private UUID testBillingAddressId;
    private UUID testShippingAddressId;
    private Order testOrder;
    private OrderItem testOrderItem;
    private List<String> testCouponCodes;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCartId = UUID.randomUUID();
        testBillingAddressId = UUID.randomUUID();
        testShippingAddressId = UUID.randomUUID();
        testCouponCodes = List.of("SAVE10", "SUMMER20");

        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setUserId(testUserId);
        testOrder.setCartId(testCartId);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setTax(BigDecimal.valueOf(10.00));
        testOrder.setShippingCost(BigDecimal.valueOf(5.00));
        testOrder.setDiscount(BigDecimal.ZERO);
        testOrder.setBillingAddressId(testBillingAddressId);
        testOrder.setShippingAddressId(testShippingAddressId);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setId(UUID.randomUUID());
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProductId(UUID.randomUUID());
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(50.00));
        testOrderItem.setDiscount(BigDecimal.ZERO);

        testOrder.setItems(List.of(testOrderItem));
    }

    @Test
    void createOrderWithDiscounts_WithCouponCodes_StoresCouponCodesAndCreatesOrder() throws Exception {
        // Given
        String userId = testUserId.toString();
        String couponCodesJson = "[\"SAVE10\", \"SUMMER20\"]";

        when(objectMapper.writeValueAsString(testCouponCodes)).thenReturn(couponCodesJson);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Mock the inherited createOrder method behavior
        // Since we can't easily mock the parent class, we'll focus on the discount-specific logic

        // When
        Order createdOrder = enhancedOrderService.createOrderWithDiscounts(
                userId, testCartId, testBillingAddressId, testShippingAddressId, testCouponCodes);

        // Then
        verify(objectMapper).writeValueAsString(testCouponCodes);
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
        // Note: Full integration testing would be needed to verify the complete flow
    }

    @Test
    void createOrderWithDiscounts_WithNullCouponCodes_CreatesOrderWithoutCoupons() throws JsonProcessingException {
        // Given
        String userId = testUserId.toString();
        List<String> nullCoupons = null;

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order createdOrder = enhancedOrderService.createOrderWithDiscounts(
                userId, testCartId, testBillingAddressId, testShippingAddressId, nullCoupons);

        // Then
        verify(objectMapper, never()).writeValueAsString(any());
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
    }

    @Test
    void createOrderWithDiscounts_WithEmptyCouponCodes_CreatesOrderWithoutCoupons() throws JsonProcessingException {
        // Given
        String userId = testUserId.toString();
        List<String> emptyCoupons = Collections.emptyList();

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order createdOrder = enhancedOrderService.createOrderWithDiscounts(
                userId, testCartId, testBillingAddressId, testShippingAddressId, emptyCoupons);

        // Then
        verify(objectMapper, never()).writeValueAsString(any());
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
    }

    @Test
    void applyDiscountsToOrder_WithSuccessfulDiscountCalculation_AppliesDiscountsCorrectly() throws Exception {
        // Given
        BigDecimal originalAmount = BigDecimal.valueOf(100.00);
        BigDecimal productDiscount = BigDecimal.valueOf(10.00);
        BigDecimal orderLevelDiscount = BigDecimal.valueOf(5.00);
        BigDecimal couponDiscount = BigDecimal.valueOf(15.00);
        BigDecimal tierDiscount = BigDecimal.valueOf(10.00);
        BigDecimal finalAmount = BigDecimal.valueOf(60.00);

        DiscountCalculationResponse successfulResponse = DiscountCalculationResponse.builder()
                .correlationId(UUID.randomUUID().toString())
                .orderId(testOrderId)
                .originalAmount(originalAmount)
                .productDiscount(productDiscount)
                .orderLevelDiscount(orderLevelDiscount)
                .couponDiscount(couponDiscount)
                .tierDiscount(tierDiscount)
                .finalAmount(finalAmount)
                .breakdown(createTestDiscountBreakdown())
                .success(true)
                .build();

        CompletableFuture<DiscountCalculationResponse> future = CompletableFuture.completedFuture(successfulResponse);

        when(discountCalculationService.calculateOrderDiscounts(any())).thenReturn(future);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(objectMapper.writeValueAsString(testCouponCodes)).thenReturn("[\"SAVE10\", \"SUMMER20\"]");
        when(objectMapper.writeValueAsString(any(List.class))).thenReturn("breakdown_json");

        // When
        Order result = enhancedOrderService.applyDiscountsToOrder(testOrder, testCouponCodes);

        // Then
        verify(discountCalculationService).calculateOrderDiscounts(any());
        verify(orderRepository).save(any(Order.class));
        verify(discountApplicationRepository).saveAll(any());
        verify(kafkaTemplate).send(eq("coupon-usage-notification"), any());
    }

    @Test
    void applyDiscountsToOrder_WithFailedDiscountCalculation_SavesOrderWithoutDiscounts() throws Exception {
        // Given
        DiscountCalculationResponse failedResponse = DiscountCalculationResponse.builder()
                .correlationId(UUID.randomUUID().toString())
                .orderId(testOrderId)
                .success(false)
                .errorMessage("Discount calculation failed")
                .build();

        CompletableFuture<DiscountCalculationResponse> future = CompletableFuture.completedFuture(failedResponse);

        when(discountCalculationService.calculateOrderDiscounts(any())).thenReturn(future);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = enhancedOrderService.applyDiscountsToOrder(testOrder, testCouponCodes);

        // Then
        verify(discountCalculationService).calculateOrderDiscounts(any());
        verify(orderRepository).save(testOrder);
        verifyNoInteractions(discountApplicationRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void applyDiscountsToOrder_WithZeroSubtotal_ReturnsOrderImmediately() {
        // Given
        testOrder.setTotalAmount(BigDecimal.ZERO);
        testOrder.setItems(Collections.emptyList());

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = enhancedOrderService.applyDiscountsToOrder(testOrder, testCouponCodes);

        // Then
        verify(orderRepository).save(testOrder);
        verifyNoInteractions(discountCalculationService);
        verifyNoInteractions(discountApplicationRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void applyDiscountsToOrder_WithTimeout_SavesOrderWithoutDiscounts() throws Exception {
        // Given
        CompletableFuture<DiscountCalculationResponse> timeoutFuture = new CompletableFuture<>();
        // Don't complete the future to simulate timeout

        when(discountCalculationService.calculateOrderDiscounts(any())).thenReturn(timeoutFuture);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = enhancedOrderService.applyDiscountsToOrder(testOrder, testCouponCodes);

        // Then
        verify(discountCalculationService).calculateOrderDiscounts(any());
        verify(orderRepository).save(testOrder);
        verifyNoInteractions(discountApplicationRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void applyDiscountsToOrder_WithException_SavesOrderWithoutDiscounts() throws Exception {
        // Given
        when(discountCalculationService.calculateOrderDiscounts(any()))
                .thenThrow(new RuntimeException("Service unavailable"));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        Order result = enhancedOrderService.applyDiscountsToOrder(testOrder, testCouponCodes);

        // Then
        verify(discountCalculationService).calculateOrderDiscounts(any());
        verify(orderRepository).save(testOrder);
        verifyNoInteractions(discountApplicationRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void applyDiscountsToOrder_WithNullCoupons_DoesNotSendUsageNotification() throws Exception {
        // Given
        DiscountCalculationResponse successfulResponse = DiscountCalculationResponse.builder()
                .correlationId(UUID.randomUUID().toString())
                .orderId(testOrderId)
                .originalAmount(BigDecimal.valueOf(100.00))
                .productDiscount(BigDecimal.valueOf(10.00))
                .orderLevelDiscount(BigDecimal.valueOf(5.00))
                .couponDiscount(BigDecimal.ZERO) // No coupon discount
                .tierDiscount(BigDecimal.valueOf(5.00))
                .finalAmount(BigDecimal.valueOf(80.00))
                .breakdown(createTestDiscountBreakdown())
                .success(true)
                .build();

        CompletableFuture<DiscountCalculationResponse> future = CompletableFuture.completedFuture(successfulResponse);

        when(discountCalculationService.calculateOrderDiscounts(any())).thenReturn(future);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(objectMapper.writeValueAsString(any(List.class))).thenReturn("breakdown_json");

        // When
        Order result = enhancedOrderService.applyDiscountsToOrder(testOrder, null);

        // Then
        verify(discountCalculationService).calculateOrderDiscounts(any());
        verify(orderRepository).save(any(Order.class));
        verify(discountApplicationRepository).saveAll(any());
        verifyNoInteractions(kafkaTemplate); // Should not send coupon usage notification
    }

    @Test
    void applyDiscountsToOrder_WithZeroCouponDiscount_DoesNotSendUsageNotification() throws Exception {
        // Given
        DiscountCalculationResponse responseWithZeroCouponDiscount = DiscountCalculationResponse.builder()
                .correlationId(UUID.randomUUID().toString())
                .orderId(testOrderId)
                .originalAmount(BigDecimal.valueOf(100.00))
                .productDiscount(BigDecimal.valueOf(10.00))
                .orderLevelDiscount(BigDecimal.valueOf(5.00))
                .couponDiscount(BigDecimal.ZERO) // Zero coupon discount
                .tierDiscount(BigDecimal.valueOf(5.00))
                .finalAmount(BigDecimal.valueOf(80.00))
                .breakdown(createTestDiscountBreakdown())
                .success(true)
                .build();

        CompletableFuture<DiscountCalculationResponse> future = CompletableFuture.completedFuture(responseWithZeroCouponDiscount);

        when(discountCalculationService.calculateOrderDiscounts(any())).thenReturn(future);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(objectMapper.writeValueAsString(testCouponCodes)).thenReturn("[\"SAVE10\", \"SUMMER20\"]");
        when(objectMapper.writeValueAsString(any(List.class))).thenReturn("breakdown_json");

        // When
        Order result = enhancedOrderService.applyDiscountsToOrder(testOrder, testCouponCodes);

        // Then
        verify(discountCalculationService).calculateOrderDiscounts(any());
        verify(orderRepository).save(any(Order.class));
        verify(discountApplicationRepository).saveAll(any());
        verifyNoInteractions(kafkaTemplate); // Should not send coupon usage notification
    }

    private List<DiscountBreakdown> createTestDiscountBreakdown() {
        return List.of(
                DiscountBreakdown.builder()
                        .discountType("PRODUCT")
                        .description("Product discount")
                        .amount(BigDecimal.valueOf(10.00))
                        .source("Product Service")
                        .build(),
                DiscountBreakdown.builder()
                        .discountType("ORDER_LEVEL")
                        .description("Order level discount")
                        .amount(BigDecimal.valueOf(5.00))
                        .source("Order Service")
                        .build(),
                DiscountBreakdown.builder()
                        .discountType("LOYALTY_COUPON")
                        .description("Loyalty coupon discount")
                        .amount(BigDecimal.valueOf(15.00))
                        .source("Loyalty Service")
                        .build(),
                DiscountBreakdown.builder()
                        .discountType("TIER_BENEFIT")
                        .description("Tier benefit discount")
                        .amount(BigDecimal.valueOf(10.00))
                        .source("Loyalty Service")
                        .build()
        );
    }
}