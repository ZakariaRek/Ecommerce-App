package com.Ecommerce.Order_Service.Listeners.AsyncComm;

import com.Ecommerce.Order_Service.Payload.Kafka.DiscountCalculationContext;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.TierDiscountRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.CouponValidationResponse;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountBreakdown;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountCalculationResponse;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.TierDiscountResponse;
import com.Ecommerce.Order_Service.Repositories.DiscountApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscountResponseListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DiscountApplicationRepository discountApplicationRepository;

    @KafkaListener(topics = "coupon-validation-response", groupId = "order-service-group")
    public void handleCouponValidationResponse(ConsumerRecord<String, Object> record) {
        try {
            CouponValidationResponse response = objectMapper.convertValue(
                    record.value(), CouponValidationResponse.class);

            String correlationId = response.getCorrelationId();
            log.info("🛒 ORDER SERVICE: Received coupon validation response for correlation: {}", correlationId);

            DiscountCalculationContext context = getContext(correlationId);

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

            DiscountCalculationContext context = getContext(correlationId);

            if (context == null) {
                log.warn("🛒 ORDER SERVICE: No context found for correlation ID: {}", correlationId);
                return;
            }

            // Final calculation
            BigDecimal tierDiscount = response.getDiscountAmount();
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
            completeDiscountCalculation(correlationId, finalResponse);

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

        redisTemplate.opsForValue().set(
                "discount-context:" + originalRequest.getCorrelationId(),
                context,
                Duration.ofMinutes(5)
        );

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
                    .description("Membership tier benefit (" + tierResponse.getTier() + ")")
                    .amount(tierDiscount)
                    .source("Loyalty Service")
                    .build());
        }

        return breakdown;
    }

    private DiscountCalculationContext getContext(String correlationId) {
        return (DiscountCalculationContext) redisTemplate
                .opsForValue().get("discount-context:" + correlationId);
    }

    private void completeDiscountCalculation(String correlationId,
                                             DiscountCalculationResponse response) {
        @SuppressWarnings("unchecked")
        CompletableFuture<DiscountCalculationResponse> future =
                (CompletableFuture<DiscountCalculationResponse>) redisTemplate
                        .opsForValue().get("discount-calculation:" + correlationId);

        if (future != null) {
            future.complete(response);
            log.info("🛒 ORDER SERVICE: Discount calculation completed for correlation: {}", correlationId);

            // Clean up Redis
            redisTemplate.delete("discount-calculation:" + correlationId);
            redisTemplate.delete("discount-context:" + correlationId);
        }
    }
}
