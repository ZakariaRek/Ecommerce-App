//package com.Ecommerce.Order_Service.Services;
//
//import com.Ecommerce.Order_Service.Entities.Order;
//import com.Ecommerce.Order_Service.Entities.OrderStatus;
//import com.Ecommerce.Order_Service.Payload.Response.payment.PaymentResponseDto;
//import com.Ecommerce.Order_Service.Repositories.OrderRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
//import org.springframework.http.*;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.ResourceAccessException;
//import org.springframework.web.client.RestTemplate;
//
//import java.math.BigDecimal;
//import java.net.URI;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PaymentIntegrationServiceTest {
//
//    @Mock
//    private RestTemplate restTemplate;
//
//    @Mock
//    private ObjectMapper objectMapper;
//
//    @Mock
//    private OrderRepository orderRepository;
//
//    @Mock
//    private DiscoveryClient discoveryClient;
//
//    @Mock
//    private ServiceInstance serviceInstance;
//
//    @InjectMocks
//    private PaymentIntegrationService paymentIntegrationService;
//
//    private UUID testOrderId;
//    private UUID testUserId;
//    private Order testOrder;
//    private String testPaymentServiceUrl;
//
//    @BeforeEach
//    void setUp() {
//        testOrderId = UUID.randomUUID();
//        testUserId = UUID.randomUUID();
//        testPaymentServiceUrl = "http://localhost:8089";
//
//        testOrder = new Order();
//        testOrder.setId(testOrderId);
//        testOrder.setUserId(testUserId);
//        testOrder.setStatus(OrderStatus.PENDING);
//        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
//
//        // Set private fields using reflection
//        ReflectionTestUtils.setField(paymentIntegrationService, "paymentServiceName", "payment-service");
//        ReflectionTestUtils.setField(paymentIntegrationService, "fallbackPaymentServiceUrl", testPaymentServiceUrl);
//    }
//
//    @Test
//    void processOrderPayment_WithValidOrderAndServiceDiscovery_ProcessesSuccessfully() {
//        // Given
//        String paymentMethod = "CREDIT_CARD";
//        BigDecimal amount = BigDecimal.valueOf(100.00);
//
//        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
//                .paymentId("pay_123")
//                .orderId(testOrderId.toString())
//                .status("COMPLETED")
//                .success(true)
//                .amount(amount)
//                .message("Payment successful")
//                .build();
//
//        // Mock service discovery
//        when(discoveryClient.getInstances("payment-service")).thenReturn(List.of(serviceInstance));
//        when(serviceInstance.getUri()).thenReturn(URI.create(testPaymentServiceUrl));
//
//        // Mock health check
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
//
//        // Mock order repository
//        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
//
//        // Mock payment processing
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));
//
//        // When
//        PaymentResponseDto result = paymentIntegrationService.processOrderPayment(testOrderId, paymentMethod, amount);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getPaymentId()).isEqualTo("pay_123");
//        assertThat(result.isSuccess()).isTrue();
//        assertThat(result.getAmount()).isEqualByComparingTo(amount);
//
//        verify(orderRepository).findById(testOrderId);
//        verify(restTemplate).exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        );
//    }
//
//    @Test
//    void processOrderPayment_WithFallbackUrl_ProcessesSuccessfully() {
//        // Given
//        String paymentMethod = "CREDIT_CARD";
//        BigDecimal amount = BigDecimal.valueOf(100.00);
//
//        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
//                .paymentId("pay_123")
//                .orderId(testOrderId.toString())
//                .status("COMPLETED")
//                .success(true)
//                .amount(amount)
//                .message("Payment successful")
//                .build();
//
//        // Mock service discovery failure
//        when(discoveryClient.getInstances("payment-service")).thenReturn(Collections.emptyList());
//
//        // Mock health check on fallback URL
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
//
//        // Mock order repository
//        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
//
//        // Mock payment processing
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));
//
//        // When
//        PaymentResponseDto result = paymentIntegrationService.processOrderPayment(testOrderId, paymentMethod, amount);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getPaymentId()).isEqualTo("pay_123");
//        assertThat(result.isSuccess()).isTrue();
//    }
//
//    @Test
//    void processOrderPayment_WithOrderNotFound_ThrowsRuntimeException() {
//        // Given
//        String paymentMethod = "CREDIT_CARD";
//        BigDecimal amount = BigDecimal.valueOf(100.00);
//
//        when(orderRepository.findById(testOrderId)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() ->
//                paymentIntegrationService.processOrderPayment(testOrderId, paymentMethod, amount)
//        ).isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Order not found");
//
//        verify(orderRepository).findById(testOrderId);
//        verifyNoInteractions(discoveryClient);
//        verifyNoInteractions(restTemplate);
//    }
//
//    @Test
//    void processOrderPayment_WithInvalidOrderStatus_ThrowsRuntimeException() {
//        // Given
//        String paymentMethod = "CREDIT_CARD";
//        BigDecimal amount = BigDecimal.valueOf(100.00);
//
//        testOrder.setStatus(OrderStatus.SHIPPED);
//        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
//
//        // When & Then
//        assertThatThrownBy(() ->
//                paymentIntegrationService.processOrderPayment(testOrderId, paymentMethod, amount)
//        ).isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Order is not in pending status");
//
//        verify(orderRepository).findById(testOrderId);
//        verifyNoInteractions(discoveryClient);
//        verifyNoInteractions(restTemplate);
//    }
//
//    @Test
//    void processOrderPayment_WithServiceUnavailable_ThrowsRuntimeException() {
//        // Given
//        String paymentMethod = "CREDIT_CARD";
//        BigDecimal amount = BigDecimal.valueOf(100.00);
//
//        // Mock service discovery failure
//        when(discoveryClient.getInstances("payment-service")).thenReturn(Collections.emptyList());
//
//        // Mock health check failure
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenThrow(new ResourceAccessException("Connection refused"));
//
//        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
//
//        // When & Then
//        assertThatThrownBy(() ->
//                paymentIntegrationService.processOrderPayment(testOrderId, paymentMethod, amount)
//        ).isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Payment Service is not available");
//
//        verify(orderRepository).findById(testOrderId);
//    }
//
//    @Test
//    void processOrderPayment_WithHttpClientError_ThrowsRuntimeException() {
//        // Given
//        String paymentMethod = "CREDIT_CARD";
//        BigDecimal amount = BigDecimal.valueOf(100.00);
//
//        // Mock service discovery
//        when(discoveryClient.getInstances("payment-service")).thenReturn(List.of(serviceInstance));
//        when(serviceInstance.getUri()).thenReturn(URI.create(testPaymentServiceUrl));
//
//        // Mock health check
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
//
//        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
//
//        // Mock HTTP client error
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid payment data".getBytes(), null));
//
//        // When & Then
//        assertThatThrownBy(() ->
//                paymentIntegrationService.processOrderPayment(testOrderId, paymentMethod, amount)
//        ).isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Payment Service HTTP error: 400 Bad Request");
//
//        verify(orderRepository).findById(testOrderId);
//    }
//
//    @Test
//    void processOrderPayment_WithRetry_SucceedsOnSecondAttempt() {
//        // Given
//        String paymentMethod = "CREDIT_CARD";
//        BigDecimal amount = BigDecimal.valueOf(100.00);
//
//        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
//                .paymentId("pay_123")
//                .orderId(testOrderId.toString())
//                .status("COMPLETED")
//                .success(true)
//                .amount(amount)
//                .build();
//
//        // Mock service discovery
//        when(discoveryClient.getInstances("payment-service")).thenReturn(List.of(serviceInstance));
//        when(serviceInstance.getUri()).thenReturn(URI.create(testPaymentServiceUrl));
//
//        // Mock health check
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
//
//        when(orderRepository.findById(testOrderId)).thenReturn(Optional.of(testOrder));
//
//        // Mock payment processing - fail first time, succeed second time
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        )).thenThrow(new ResourceAccessException("Temporary network error"))
//                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));
//
//        // When
//        PaymentResponseDto result = paymentIntegrationService.processOrderPayment(testOrderId, paymentMethod, amount);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getPaymentId()).isEqualTo("pay_123");
//        assertThat(result.isSuccess()).isTrue();
//
//        // Verify retry happened
//        verify(restTemplate, times(2)).exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        );
//    }
//
//    @Test
//    void getOrderPaymentStatus_WithValidOrderId_ReturnsStatus() {
//        // Given
//        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
//                .paymentId("pay_123")
//                .orderId(testOrderId.toString())
//                .status("COMPLETED")
//                .success(true)
//                .build();
//
//        // Mock service discovery
//        when(discoveryClient.getInstances("payment-service")).thenReturn(List.of(serviceInstance));
//        when(serviceInstance.getUri()).thenReturn(URI.create(testPaymentServiceUrl));
//
//        // Mock health check
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
//
//        // Mock payment status request
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments/status"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));
//
//        // When
//        PaymentResponseDto result = paymentIntegrationService.getOrderPaymentStatus(testOrderId);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getPaymentId()).isEqualTo("pay_123");
//        assertThat(result.getStatus()).isEqualTo("COMPLETED");
//
//        verify(restTemplate).exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/payments/status"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        );
//    }
//
//    @Test
//    void refundOrderPayment_WithValidData_ProcessesRefundSuccessfully() {
//        // Given
//        BigDecimal refundAmount = BigDecimal.valueOf(50.00);
//        String reason = "Customer request";
//
//        PaymentResponseDto expectedResponse = PaymentResponseDto.builder()
//                .paymentId("refund_123")
//                .orderId(testOrderId.toString())
//                .status("REFUNDED")
//                .success(true)
//                .amount(refundAmount)
//                .build();
//
//        // Mock service discovery
//        when(discoveryClient.getInstances("payment-service")).thenReturn(List.of(serviceInstance));
//        when(serviceInstance.getUri()).thenReturn(URI.create(testPaymentServiceUrl));
//
//        // Mock health check
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
//
//        // Mock refund request
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/refund"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        )).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));
//
//        // When
//        PaymentResponseDto result = paymentIntegrationService.refundOrderPayment(testOrderId, refundAmount, reason);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getPaymentId()).isEqualTo("refund_123");
//        assertThat(result.getStatus()).isEqualTo("REFUNDED");
//        assertThat(result.getAmount()).isEqualByComparingTo(refundAmount);
//
//        // Verify refund request body
//        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
//        verify(restTemplate).exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/refund"),
//                eq(HttpMethod.POST),
//                entityCaptor.capture(),
//                eq(PaymentResponseDto.class)
//        );
//
//        HttpEntity<?> capturedEntity = entityCaptor.getValue();
//        assertThat(capturedEntity.getBody()).isInstanceOf(PaymentIntegrationService.RefundPaymentRequestDto.class);
//    }
//
//    @Test
//    void refundOrderPayment_WithException_ThrowsRuntimeException() {
//        // Given
//        BigDecimal refundAmount = BigDecimal.valueOf(50.00);
//        String reason = "Customer request";
//
//        // Mock service discovery
//        when(discoveryClient.getInstances("payment-service")).thenReturn(List.of(serviceInstance));
//        when(serviceInstance.getUri()).thenReturn(URI.create(testPaymentServiceUrl));
//
//        // Mock health check
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/health"),
//                eq(HttpMethod.GET),
//                any(HttpEntity.class),
//                eq(String.class)
//        )).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
//
//        // Mock refund request failure
//        when(restTemplate.exchange(
//                eq(testPaymentServiceUrl + "/api/payments/orders/" + testOrderId + "/refund"),
//                eq(HttpMethod.POST),
//                any(HttpEntity.class),
//                eq(PaymentResponseDto.class)
//        )).thenThrow(new ResourceAccessException("Service unavailable"));
//
//        // When & Then
//        assertThatThrownBy(() ->
//                paymentIntegrationService.refundOrderPayment(testOrderId, refundAmount, reason)
//        ).isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("Refund processing failed");
//    }
//}