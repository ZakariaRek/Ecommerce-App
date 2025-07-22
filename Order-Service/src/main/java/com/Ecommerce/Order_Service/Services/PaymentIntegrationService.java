// Order-Service/src/main/java/com/Ecommerce/Order_Service/Services/PaymentIntegrationService.java
package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Payload.Request.payment.ProcessPaymentRequestDto;
import com.Ecommerce.Order_Service.Payload.Response.payment.PaymentResponseDto;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final DiscoveryClient discoveryClient;

    @Value("${payment.service.name:payment-service}")
    private String paymentServiceName;

    @Value("${payment.service.url:#{null}}")
    private String fallbackPaymentServiceUrl;

    /**
     * Get Payment Service URL using service discovery
     */
    private String getPaymentServiceUrl() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(paymentServiceName);
            if (!instances.isEmpty()) {
                ServiceInstance instance = instances.get(0);
                String serviceUrl = instance.getUri().toString();
                log.info("💳 Found Payment Service via discovery: {}", serviceUrl);
                return serviceUrl;
            }
        } catch (Exception e) {
            log.warn("💳 Service discovery failed for {}: {}", paymentServiceName, e.getMessage());
        }

        // Fallback to configured URL or default
        String fallbackUrl = fallbackPaymentServiceUrl != null ? fallbackPaymentServiceUrl : "http://localhost:8080";
        log.info("💳 Using fallback Payment Service URL: {}", fallbackUrl);
        return fallbackUrl;
    }

    /**
     * Process payment for an order
     */
    public PaymentResponseDto processOrderPayment(UUID orderId, String paymentMethod, BigDecimal amount) {
        try {
            log.info("💳 Processing payment for order: {}, amount: {}, method: {}", orderId, amount, paymentMethod);

            // Get order to validate
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            if (order.getStatus() != OrderStatus.PENDING) {
                throw new RuntimeException("Order is not in pending status");
            }

            // Create payment request with correct field names for Go service
            ProcessPaymentRequestDto paymentRequest = ProcessPaymentRequestDto.builder()
                    .orderId(orderId.toString())  // This matches the Go struct field "orderId"
                    .amount(amount.doubleValue())
                    .paymentMethod(paymentMethod)  // This matches the Go struct field "paymentMethod"
                    .currency("USD")
                    .build();

            // Get Payment Service URL
            String baseUrl = getPaymentServiceUrl();
            String url = baseUrl + "/api/orders/" + orderId + "/payments";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<ProcessPaymentRequestDto> entity = new HttpEntity<>(paymentRequest, headers);

            log.info("💳 Calling Payment Service: {}", url);
            log.info("💳 Payment request payload: {}", paymentRequest);

            ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaymentResponseDto.class
            );

            PaymentResponseDto paymentResponse = response.getBody();
            log.info("💳 Payment Service response: {}", paymentResponse);

            return paymentResponse;

        } catch (Exception e) {
            log.error("💳 Error processing payment for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment status for an order
     */
    public PaymentResponseDto getOrderPaymentStatus(UUID orderId) {
        try {
            String baseUrl = getPaymentServiceUrl();
            String url = baseUrl + "/api/orders/" + orderId + "/payments/status";

            log.info("💳 Getting payment status for order: {}", orderId);
            ResponseEntity<PaymentResponseDto> response = restTemplate.getForEntity(url, PaymentResponseDto.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("💳 Error getting payment status for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to get payment status: " + e.getMessage(), e);
        }
    }

    /**
     * Refund payment for an order
     */
    public PaymentResponseDto refundOrderPayment(UUID orderId, BigDecimal refundAmount, String reason) {
        try {
            log.info("💳 Processing refund for order: {}, amount: {}", orderId, refundAmount);

            // Create refund request
            RefundPaymentRequestDto refundRequest = RefundPaymentRequestDto.builder()
                    .orderId(orderId.toString())
                    .amount(refundAmount.doubleValue())
                    .reason(reason)
                    .build();

            String baseUrl = getPaymentServiceUrl();
            String url = baseUrl + "/api/orders/" + orderId + "/refund";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<RefundPaymentRequestDto> entity = new HttpEntity<>(refundRequest, headers);

            ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaymentResponseDto.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("💳 Error processing refund for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }
    }

    // Inner classes for request DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RefundPaymentRequestDto {
        private String orderId;
        private Double amount;
        private String reason;
    }
}