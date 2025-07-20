// Fixed Order-Service: DiscountResponseListener.java
package com.Ecommerce.Order_Service.Listeners.AsyncComm;

import com.Ecommerce.Order_Service.Payload.Kafka.DiscountCalculationContext;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.TierDiscountRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.*;
import com.Ecommerce.Order_Service.Repositories.DiscountApplicationRepository;
import com.Ecommerce.Order_Service.Services.Kafka.DiscountCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscountResponseListener {

    private final ObjectMapper objectMapper;
    private final DiscountApplicationRepository discountApplicationRepository;
    private final DiscountCalculationService discountCalculationService; // FIX: Add dependency

    @KafkaListener(topics = "combined-discount-response", groupId = "order-service-group")
    public void handleCombinedDiscountResponse(ConsumerRecord<String, Object> record) {
        try {
            log.info("🛒 ORDER SERVICE: Received raw message: {}", record.value());

            // FIX: Handle both null payload and actual response
            if (record.value() == null || "KafkaNull".equals(record.value().getClass().getSimpleName())) {
                log.warn("🛒 ORDER SERVICE: Received null payload for correlation: {}", record.key());
                return;
            }

            CombinedDiscountResponse response = objectMapper.convertValue(
                    record.value(), CombinedDiscountResponse.class);

            String correlationId = response.getCorrelationId();
            log.info("🛒 ORDER SERVICE: Received combined discount response for correlation: {}", correlationId);

            // FIX: Get context from DiscountCalculationService
            DiscountCalculationContext context = discountCalculationService.getContext(correlationId);
            if (context == null) {
                log.warn("🛒 ORDER SERVICE: No context found for correlation ID: {}", correlationId);
                log.warn("🛒 ORDER SERVICE: Available contexts: {}",
                        discountCalculationService.getAvailableContextKeys());
                return;
            }

            if (response.isSuccess()) {
                log.info("🛒 ORDER SERVICE: Combined discount calculation successful - " +
                                "Coupon discount: {}, Tier discount: {}, Total discount: {}, Final amount: {}",
                        response.getCouponDiscount(),
                        response.getTierDiscount(),
                        response.getTotalDiscount(),
                        response.getFinalAmount());

                // Create final discount calculation response
                DiscountCalculationResponse finalResponse = DiscountCalculationResponse.builder()
                        .correlationId(correlationId)
                        .orderId(response.getOrderId())
                        .originalAmount(response.getOriginalAmount())
                        .productDiscount(response.getProductDiscount())
                        .orderLevelDiscount(response.getOrderLevelDiscount())
                        .couponDiscount(response.getCouponDiscount())
                        .tierDiscount(response.getTierDiscount())
                        .finalAmount(response.getFinalAmount())
                        .breakdown(response.getBreakdown())
                        .success(true)
                        .build();

                // FIX: Complete the discount calculation using DiscountCalculationService
                discountCalculationService.completePendingCalculation(correlationId, finalResponse);

            } else {
                log.error("🛒 ORDER SERVICE: Combined discount calculation failed: {}", response.getErrorMessage());

                // Create error response
                DiscountCalculationResponse errorResponse = DiscountCalculationResponse.builder()
                        .correlationId(correlationId)
                        .orderId(response.getOrderId())
                        .success(false)
                        .errorMessage(response.getErrorMessage())
                        .build();

                discountCalculationService.completePendingCalculation(correlationId, errorResponse);
            }

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error processing combined discount response", e);

            // Try to complete with error if we have correlation ID
            String correlationId = record.key();
            if (correlationId != null) {
                DiscountCalculationResponse errorResponse = DiscountCalculationResponse.builder()
                        .correlationId(correlationId)
                        .success(false)
                        .errorMessage("Error processing response: " + e.getMessage())
                        .build();

                discountCalculationService.completePendingCalculation(correlationId, errorResponse);
            }
        }
    }

    @KafkaListener(topics = "coupon-validation-response", groupId = "order-service-group")
    public void handleCouponValidationResponse(ConsumerRecord<String, Object> record) {
        try {
            CouponValidationResponse response = objectMapper.convertValue(
                    record.value(), CouponValidationResponse.class);

            String correlationId = response.getCorrelationId();
            log.info("🛒 ORDER SERVICE: Received coupon validation response for correlation: {}", correlationId);

            DiscountCalculationContext context = discountCalculationService.getContext(correlationId);

            if (context == null) {
                log.warn("🛒 ORDER SERVICE: No context found for correlation ID: {}", correlationId);
                return;
            }

            BigDecimal couponDiscount = response.isSuccess() ?
                    response.getTotalDiscount() : BigDecimal.ZERO;

            if (response.isSuccess()) {
                log.info("🛒 ORDER SERVICE: Coupon validation successful, discount: {}", couponDiscount);
            } else {
                log.warn("🛒 ORDER SERVICE: Coupon validation failed: {}", response.getErrors());
            }

            // Request tier discount
            requestTierDiscountFromContext(context, couponDiscount);

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error processing coupon validation response", e);
        }
    }

    @KafkaListener(topics = "tier-discount-response", groupId = "order-service-group")
    public void handleTierDiscountResponse(ConsumerRecord<String, Object> record) {
        try {
            TierDiscountResponse response = objectMapper.convertValue(
                    record.value(), TierDiscountResponse.class);

            String correlationId = response.getCorrelationId();
            log.info("🛒 ORDER SERVICE: Received tier discount response for correlation: {}", correlationId);

            DiscountCalculationContext context = discountCalculationService.getContext(correlationId);
            if (context == null) {
                log.warn("🛒 ORDER SERVICE: No context found for correlation ID: {}", correlationId);
                return;
            }

            // Final calculation
            BigDecimal tierDiscount = response.getTierDiscount();
            BigDecimal finalAmount = context.getAmountAfterCouponDiscount().subtract(tierDiscount);

            log.info("🛒 ORDER SERVICE: Tier discount: {}, Final amount: {}", tierDiscount, finalAmount);

            // Create breakdown
            List<DiscountBreakdown> breakdown = createDiscountBreakdown(context, tierDiscount, response);

            DiscountCalculationResponse finalResponse = DiscountCalculationResponse.builder()
                    .correlationId(correlationId)
                    .orderId(context.getOriginalRequest().getOrderId())
                    .originalAmount(context.getOriginalRequest().getSubtotal())
                    .productDiscount(context.getProductDiscount())
                    .orderLevelDiscount(context.getOrderDiscount())
                    .couponDiscount(context.getCouponDiscount())
                    .tierDiscount(tierDiscount)
                    .finalAmount(finalAmount)
                    .breakdown(breakdown)
                    .success(true)
                    .build();

            // Complete the original future
            discountCalculationService.completePendingCalculation(correlationId, finalResponse);

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error processing tier discount response", e);
        }
    }

    private void requestTierDiscountFromContext(DiscountCalculationContext context, BigDecimal couponDiscount) {
        DiscountCalculationRequest originalRequest = context.getOriginalRequest();
        BigDecimal afterCouponDiscount = context.getAmountAfterOrderDiscount().subtract(couponDiscount);

        TierDiscountRequest tierRequest = TierDiscountRequest.builder()
                .correlationId(originalRequest.getCorrelationId())
                .userId(originalRequest.getUserId())
                .amount(afterCouponDiscount)
                .build();

        // Update context
        context.setCouponDiscount(couponDiscount);
        context.setAmountAfterCouponDiscount(afterCouponDiscount);

        log.info("🛒 ORDER SERVICE: Requesting tier discount for amount: {}", afterCouponDiscount);
    }

    private List<DiscountBreakdown> createDiscountBreakdown(DiscountCalculationContext context,
                                                            BigDecimal tierDiscount,
                                                            TierDiscountResponse tierResponse) {
        List<DiscountBreakdown> breakdown = new ArrayList<>();

        if (context.getProductDiscount().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("PRODUCT")
                    .description("Product-level discounts")
                    .amount(context.getProductDiscount())
                    .source("Product Service")
                    .build());
        }

        if (context.getOrderDiscount().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("ORDER_LEVEL")
                    .description("Order-level discounts (bulk, minimum purchase)")
                    .amount(context.getOrderDiscount())
                    .source("Order Service")
                    .build());
        }

        if (context.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("LOYALTY_COUPON")
                    .description("Loyalty coupon discounts")
                    .amount(context.getCouponDiscount())
                    .source("Loyalty Service")
                    .build());
        }

        if (tierDiscount.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.add(DiscountBreakdown.builder()
                    .discountType("TIER_BENEFIT")
                    .description("Membership tier benefit (" + tierResponse.getTierDiscount() + ")")
                    .amount(tierDiscount)
                    .source("Loyalty Service")
                    .build());
        }

        return breakdown;
    }
}