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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.ConnectException;
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
     * Get Payment Service URL using service discovery with enhanced error handling
     */
    private String getPaymentServiceUrl() {
        // First try service discovery
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(paymentServiceName);
            if (!instances.isEmpty()) {
                ServiceInstance instance = instances.get(0);
                String serviceUrl = instance.getUri().toString();
                log.info("💳 Found Payment Service via discovery: {}", serviceUrl);

                // Test connectivity to the discovered service
                if (testServiceConnectivity(serviceUrl)) {
                    return serviceUrl;
                } else {
                    log.warn("💳 Discovered Payment Service is not responding: {}", serviceUrl);
                }
            }
        } catch (Exception e) {
            log.warn("💳 Service discovery failed for {}: {}", paymentServiceName, e.getMessage());
        }

        // Fallback to configured URL or default
        String fallbackUrl = fallbackPaymentServiceUrl != null ? fallbackPaymentServiceUrl : "http://localhost:8080";
        log.info("💳 Using fallback Payment Service URL: {}", fallbackUrl);

        // Test fallback URL
        if (!testServiceConnectivity(fallbackUrl)) {
            log.error("💳 Payment Service is not available at: {}", fallbackUrl);
            throw new RuntimeException("Payment Service is not available at: " + fallbackUrl);
        }

        return fallbackUrl;
    }

    /**
     * Test if the Payment Service is reachable
     */
    private boolean testServiceConnectivity(String baseUrl) {
        try {
            String healthUrl = baseUrl + "/health";
            log.debug("💳 Testing Payment Service connectivity: {}", healthUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    healthUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            log.debug("💳 Payment Service health check result: {}", isHealthy);
            return isHealthy;

        } catch (Exception e) {
            log.debug("💳 Payment Service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Process payment for an order with enhanced error handling
     */
    public PaymentResponseDto processOrderPayment(UUID orderId, String paymentMethod, BigDecimal amount) {
        try {
            log.info("💳 Processing payment for order: {}, amount: {}, method: {}", orderId, amount, paymentMethod);

            // Get order to validate
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

            if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
                throw new RuntimeException("Order is not in pending status. Current status: " + order.getStatus());
            }

            // Create payment request with correct field names for Go service
            ProcessPaymentRequestDto paymentRequest = ProcessPaymentRequestDto.builder()
                    .orderId(orderId.toString())
                    .amount(amount.doubleValue())
                    .paymentMethod(paymentMethod)
                    .currency("USD")
                    .build();

            // Get Payment Service URL with retries
            String baseUrl = getPaymentServiceUrl();
            String url = baseUrl + "/api/orders/" + orderId + "/payments";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.set("User-Agent", "Order-Service/1.0");

            HttpEntity<ProcessPaymentRequestDto> entity = new HttpEntity<>(paymentRequest, headers);

            log.info("💳 Calling Payment Service: {}", url);
            log.info("💳 Payment request payload: {}", paymentRequest);

            // Make the request with retry logic
            PaymentResponseDto paymentResponse = makePaymentRequestWithRetry(url, entity);

            if (paymentResponse != null) {
                log.info("💳 Payment Service response: {}", paymentResponse);
                return paymentResponse;
            } else {
                throw new RuntimeException("Payment Service returned null response");
            }

        } catch (HttpClientErrorException e) {
            String errorMessage = String.format("Payment Service HTTP error: %d %s - %s",
                    e.getRawStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
            log.error("💳 {}", errorMessage);
            throw new RuntimeException(errorMessage, e);

        } catch (ResourceAccessException e) {
            String errorMessage = "Payment Service is not accessible: " + e.getMessage();
            log.error("💳 {}", errorMessage);
            throw new RuntimeException(errorMessage, e);

        } catch (Exception e) {
            log.error("💳 Error processing payment for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Make payment request with retry logic
     */
    private PaymentResponseDto makePaymentRequestWithRetry(String url, HttpEntity<ProcessPaymentRequestDto> entity) {
        int maxRetries = 3;
        int retryDelay = 1000; // 1 second

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("💳 Payment request attempt {}/{}: {}", attempt, maxRetries, url);

                ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        PaymentResponseDto.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("💳 Payment request successful on attempt {}", attempt);
                    return response.getBody();
                }

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.error("💳 Payment endpoint not found (404) - this suggests Payment Service is running but endpoint is missing");
                    log.error("💳 Expected endpoint: {}", url);
                    log.error("💳 Response body: {}", e.getResponseBodyAsString());
                }

                if (attempt == maxRetries) {
                    throw e; // Re-throw on final attempt
                }

                log.warn("💳 Payment request failed on attempt {} with HTTP error: {} - {}",
                        attempt, e.getStatusCode(), e.getMessage());

            } catch (ResourceAccessException e) {
                if (attempt == maxRetries) {
                    throw e; // Re-throw on final attempt
                }

                log.warn("💳 Payment request failed on attempt {} with connectivity error: {}",
                        attempt, e.getMessage());
            }

            // Wait before retry
            if (attempt < maxRetries) {
                try {
                    log.info("💳 Waiting {} ms before retry...", retryDelay);
                    Thread.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Payment request interrupted", ie);
                }
            }
        }

        throw new RuntimeException("Payment request failed after " + maxRetries + " attempts");
    }

    /**
     * Get payment status for an order
     */
    public PaymentResponseDto getOrderPaymentStatus(UUID orderId) {
        try {
            String baseUrl = getPaymentServiceUrl();
            String url = baseUrl + "/api/orders/" + orderId + "/payments/status";

            log.info("💳 Getting payment status for order: {} from {}", orderId, url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PaymentResponseDto.class
            );

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