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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @Value("${payment.service.url:http://localhost:8080}")
    private String paymentServiceUrl;

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

            // Create payment request with all required fields populated
            ProcessPaymentRequestDto paymentRequest = ProcessPaymentRequestDto.builder()
                    .orderId(orderId.toString())
                    .amount(amount.doubleValue())
                    .paymentMethod(paymentMethod)
                    .currency("USD")
                    .build();

            // Call Payment Service
            String url = paymentServiceUrl + "/api/orders/" + orderId + "/payments";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

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
            String url = paymentServiceUrl + "/api/orders/" + orderId + "/payments/status";

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

            String url = paymentServiceUrl + "/api/orders/" + orderId + "/refund";
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