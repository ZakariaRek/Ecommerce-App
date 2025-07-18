package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.DiscountRule;
import com.Ecommerce.Order_Service.Payload.Kafka.DiscountCalculationContext;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.CouponValidationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.TierDiscountRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountCalculationResponse;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import com.Ecommerce.Order_Service.Repositories.DiscountRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCalculationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DiscountRuleRepository discountRuleRepository;
    private final ObjectMapper objectMapper;

    public CompletableFuture<DiscountCalculationResponse> calculateOrderDiscounts(
            DiscountCalculationRequest request) {

        String correlationId = request.getCorrelationId();
        CompletableFuture<DiscountCalculationResponse> future = new CompletableFuture<>();

        // Store the future for later completion
        redisTemplate.opsForValue().set(
                "discount-calculation:" + correlationId,
                future,
                Duration.ofMinutes(5)
        );

        log.info("🛒 ORDER SERVICE: Starting discount calculation for order {} with correlation {}",
                request.getOrderId(), correlationId);

        // Start the discount calculation process
        processDiscountCalculation(request);

        return future;
    }

    private void processDiscountCalculation(DiscountCalculationRequest request) {
        try {
            log.info("🛒 ORDER SERVICE: Processing discount calculation for order {}", request.getOrderId());

            // Step 1: Calculate product-level discounts (already applied at product level)
            BigDecimal productDiscount = calculateProductDiscounts(request.getItems());
            log.info("🛒 ORDER SERVICE: Product discount calculated: {}", productDiscount);

            // Step 2: Calculate order-level discounts
            BigDecimal orderDiscount = calculateOrderLevelDiscounts(request);
            log.info("🛒 ORDER SERVICE: Order-level discount calculated: {}", orderDiscount);

            BigDecimal afterOrderDiscount = request.getSubtotal()
                    .subtract(productDiscount)
                    .subtract(orderDiscount);

            // Step 3: Request coupon validation from Loyalty Service
            if (request.getCouponCodes() != null && !request.getCouponCodes().isEmpty()) {
                log.info("🛒 ORDER SERVICE: Requesting coupon validation for codes: {}", request.getCouponCodes());
                requestCouponValidation(request, afterOrderDiscount, productDiscount, orderDiscount);
            } else {
                // Skip coupon validation, go to tier discount
                log.info("🛒 ORDER SERVICE: No coupons provided, proceeding to tier discount");
                requestTierDiscount(request, afterOrderDiscount, productDiscount, orderDiscount, BigDecimal.ZERO);
            }

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error in discount calculation process", e);
            completeWithError(request.getCorrelationId(), "Discount calculation failed: " + e.getMessage());
        }
    }

    private void requestCouponValidation(DiscountCalculationRequest request,
                                         BigDecimal afterOrderDiscount,
                                         BigDecimal productDiscount,
                                         BigDecimal orderDiscount) {

        CouponValidationRequest couponRequest = CouponValidationRequest.builder()
                .correlationId(request.getCorrelationId())
                .userId(request.getUserId())
                .couponCodes(request.getCouponCodes())
                .amount(afterOrderDiscount)
                .orderId(request.getOrderId())
                .build();

        // Store context for when response comes back
        DiscountCalculationContext context = DiscountCalculationContext.builder()
                .originalRequest(request)
                .productDiscount(productDiscount)
                .orderDiscount(orderDiscount)
                .amountAfterOrderDiscount(afterOrderDiscount)
                .build();

        redisTemplate.opsForValue().set(
                "discount-context:" + request.getCorrelationId(),
                context,
                Duration.ofMinutes(5)
        );

        log.info("🛒 ORDER SERVICE: Sending coupon validation request to Loyalty Service");
        kafkaTemplate.send("coupon-validation-request",
                request.getCorrelationId(), couponRequest);
    }

    private void requestTierDiscount(DiscountCalculationRequest request,
                                     BigDecimal afterOrderDiscount,
                                     BigDecimal productDiscount,
                                     BigDecimal orderDiscount,
                                     BigDecimal couponDiscount) {

        BigDecimal afterCouponDiscount = afterOrderDiscount.subtract(couponDiscount);

        TierDiscountRequest tierRequest = TierDiscountRequest.builder()
                .correlationId(request.getCorrelationId())
                .userId(request.getUserId())
                .amount(afterCouponDiscount)
                .build();

        // Update context with coupon discount
        DiscountCalculationContext context = (DiscountCalculationContext) redisTemplate
                .opsForValue().get("discount-context:" + request.getCorrelationId());

        if (context != null) {
            context.setCouponDiscount(couponDiscount);
            context.setAmountAfterCouponDiscount(afterCouponDiscount);

            redisTemplate.opsForValue().set(
                    "discount-context:" + request.getCorrelationId(),
                    context,
                    Duration.ofMinutes(5)
            );
        }

        log.info("🛒 ORDER SERVICE: Sending tier discount request to Loyalty Service");
        kafkaTemplate.send("tier-discount-request",
                request.getCorrelationId(), tierRequest);
    }

    private BigDecimal calculateOrderLevelDiscounts(DiscountCalculationRequest request) {
        BigDecimal discount = BigDecimal.ZERO;

        // Get active discount rules
        List<DiscountRule> activeRules = discountRuleRepository.findByActiveTrue();

        for (DiscountRule rule : activeRules) {
            try {
                BigDecimal ruleDiscount = applyDiscountRule(rule, request);
                discount = discount.add(ruleDiscount);

                if (ruleDiscount.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("🛒 ORDER SERVICE: Applied rule '{}': {}", rule.getRuleName(), ruleDiscount);
                }
            } catch (Exception e) {
                log.warn("🛒 ORDER SERVICE: Failed to apply rule '{}': {}", rule.getRuleName(), e.getMessage());
            }
        }

        return discount;
    }

    private BigDecimal applyDiscountRule(DiscountRule rule, DiscountCalculationRequest request) {
        try {
            Map<String, Object> conditions = objectMapper.readValue(rule.getConditions(), Map.class);
            Map<String, Object> config = objectMapper.readValue(rule.getDiscountConfig(), Map.class);

            // Check if conditions are met
            if (!areConditionsMet(conditions, request)) {
                return BigDecimal.ZERO;
            }

            // Calculate discount based on type
            String discountType = (String) config.get("type");
            Number discountValue = (Number) config.get("value");
            Number maxDiscount = (Number) config.get("max_discount");

            BigDecimal calculatedDiscount = BigDecimal.ZERO;

            if ("PERCENTAGE".equals(discountType)) {
                calculatedDiscount = request.getSubtotal()
                        .multiply(BigDecimal.valueOf(discountValue.doubleValue() / 100));
            } else if ("FIXED_AMOUNT".equals(discountType)) {
                calculatedDiscount = BigDecimal.valueOf(discountValue.doubleValue());
            }

            // Apply max discount limit
            if (maxDiscount != null &&
                    calculatedDiscount.compareTo(BigDecimal.valueOf(maxDiscount.doubleValue())) > 0) {
                calculatedDiscount = BigDecimal.valueOf(maxDiscount.doubleValue());
            }

            return calculatedDiscount;

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error applying discount rule {}: {}", rule.getRuleName(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private boolean areConditionsMet(Map<String, Object> conditions, DiscountCalculationRequest request) {
        // Bulk discount condition
        if (conditions.containsKey("min_items")) {
            int minItems = ((Number) conditions.get("min_items")).intValue();
            if (request.getTotalItems() < minItems) {
                return false;
            }
        }

        // Minimum purchase amount condition
        if (conditions.containsKey("min_amount")) {
            double minAmount = ((Number) conditions.get("min_amount")).doubleValue();
            if (request.getSubtotal().compareTo(BigDecimal.valueOf(minAmount)) < 0) {
                return false;
            }
        }

        return true;
    }

    private BigDecimal calculateProductDiscounts(List<OrderItemResponseDto> items) {
        if (items == null) return BigDecimal.ZERO;

        return items.stream()
                .map(item -> item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void completeWithError(String correlationId, String errorMessage) {
        @SuppressWarnings("unchecked")
        CompletableFuture<DiscountCalculationResponse> future =
                (CompletableFuture<DiscountCalculationResponse>) redisTemplate
                        .opsForValue().get("discount-calculation:" + correlationId);

        if (future != null) {
            DiscountCalculationResponse errorResponse = DiscountCalculationResponse.builder()
                    .correlationId(correlationId)
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();

            future.complete(errorResponse);

            // Clean up Redis
            redisTemplate.delete("discount-calculation:" + correlationId);
            redisTemplate.delete("discount-context:" + correlationId);
        }
    }
}
