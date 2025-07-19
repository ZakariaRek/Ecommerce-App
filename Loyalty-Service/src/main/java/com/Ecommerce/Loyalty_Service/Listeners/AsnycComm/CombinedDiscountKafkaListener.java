// Loyalty-Service: Combined Discount Kafka Listener
package com.Ecommerce.Loyalty_Service.Listeners.AsnycComm;

import com.Ecommerce.Loyalty_Service.Payload.Kafka.Request.CombinedDiscountRequest;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CombinedDiscountResponse;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CombinedDiscountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CombinedDiscountKafkaListener {

    private final CombinedDiscountService combinedDiscountService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Handle combined discount requests from Order Service
     * This replaces separate coupon-validation-request and tier-discount-request listeners
     */
    @KafkaListener(topics = "combined-discount-request", groupId = "loyalty-service-group")
    public void handleCombinedDiscountRequest(ConsumerRecord<String, Object> record) {
        try {
            CombinedDiscountRequest request = objectMapper.convertValue(
                    record.value(), CombinedDiscountRequest.class);

            log.info("💎 LOYALTY SERVICE: Received combined discount request for user {} with correlation {}",
                    request.getUserId(), request.getCorrelationId());

            log.info("💎 LOYALTY SERVICE: Request details - Amount: {}, Coupons: {}, Items: {}",
                    request.getAmountAfterOrderDiscounts(),
                    request.getCouponCodes(),
                    request.getTotalItems());

            // Process both coupon validation and tier discount calculation
            CombinedDiscountResponse response = combinedDiscountService
                    .calculateCombinedDiscounts(request);

            // Log the results
            if (response.isSuccess()) {
                log.info("💎 LOYALTY SERVICE: Combined discount calculation successful - " +
                                "Coupon discount: {}, Tier discount: {}, Total loyalty discount: {}",
                        response.getCouponDiscount(),
                        response.getTierDiscount(),
                        response.getTotalDiscount());

                if (response.getCouponErrors() != null && !response.getCouponErrors().isEmpty()) {
                    log.warn("💎 LOYALTY SERVICE: Some coupon validation issues: {}", response.getCouponErrors());
                }
            } else {
                log.error("💎 LOYALTY SERVICE: Combined discount calculation failed: {}", response.getErrorMessage());
            }

            // Send response back to Order Service
            log.info("💎 LOYALTY SERVICE: Sending combined discount response for correlation: {}",
                    request.getCorrelationId());

            kafkaTemplate.send("combined-discount-response",
                            request.getCorrelationId(), response)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("💎 LOYALTY SERVICE: Failed to send combined discount response", ex);
                        } else {
                            log.info("💎 LOYALTY SERVICE: Successfully sent combined discount response");
                        }
                    });

        } catch (Exception e) {
            log.error("💎 LOYALTY SERVICE: Error processing combined discount request", e);

            // Send error response
            try {
                String correlationId = record.key();
                CombinedDiscountResponse errorResponse = CombinedDiscountResponse.builder()
                        .correlationId(correlationId)
                        .success(false)
                        .errorMessage("Internal error processing discount request: " + e.getMessage())
                        .couponDiscount(BigDecimal.ZERO)
                        .tierDiscount(BigDecimal.ZERO)
                        .totalDiscount(BigDecimal.ZERO)
                        .build();

                kafkaTemplate.send("combined-discount-response", correlationId, errorResponse);
            } catch (Exception sendError) {
                log.error("💎 LOYALTY SERVICE: Failed to send error response", sendError);
            }
        }
    }

    /**
     * Optional: Keep the existing separate listeners for backward compatibility
     * You can remove these if you want to force all requests to use the new combined flow
     */
    @Deprecated
    @KafkaListener(topics = "coupon-validation-request", groupId = "loyalty-service-group")
    public void handleLegacyCouponValidationRequest(ConsumerRecord<String, Object> record) {
        log.warn("💎 LOYALTY SERVICE: Received legacy coupon validation request. " +
                "Consider migrating to combined-discount-request for better performance.");

        // You could either:
        // 1. Process it normally (backward compatibility)
        // 2. Convert it to a combined request and process
        // 3. Return an error encouraging migration

        log.info("💎 LOYALTY SERVICE: Processing legacy request with fallback to separate flows");
        // For now, just log and ignore - you can implement fallback logic if needed
    }

    @Deprecated
    @KafkaListener(topics = "tier-discount-request", groupId = "loyalty-service-group")
    public void handleLegacyTierDiscountRequest(ConsumerRecord<String, Object> record) {
        log.warn("💎 LOYALTY SERVICE: Received legacy tier discount request. " +
                "Consider migrating to combined-discount-request for better performance.");

        log.info("💎 LOYALTY SERVICE: Processing legacy request with fallback to separate flows");
        // For now, just log and ignore - you can implement fallback logic if needed
    }
}