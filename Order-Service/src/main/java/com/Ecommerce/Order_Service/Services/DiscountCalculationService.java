package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Payload.Kafka.DiscountCalculationContext;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.CouponValidationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.TierDiscountRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountCalculationResponse;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountCalculationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // In-memory storage for correlation contexts
    private final Map<String, CompletableFuture<DiscountCalculationResponse>> pendingCalculations = new ConcurrentHashMap<>();
    private final Map<String, DiscountCalculationContext> contexts = new ConcurrentHashMap<>();

    public CompletableFuture<DiscountCalculationResponse> calculateOrderDiscounts(
            DiscountCalculationRequest request) {

        String correlationId = request.getCorrelationId();
        CompletableFuture<DiscountCalculationResponse> future = new CompletableFuture<>();

        // Store the future for later completion
        pendingCalculations.put(correlationId, future);

        log.info("🛒 ORDER SERVICE: Starting discount calculation for order {} with correlation {}",
                request.getOrderId(), correlationId);

        // Start the discount calculation process
        processDiscountCalculation(request);

        return future;
    }

    private void processDiscountCalculation(DiscountCalculationRequest request) {
        try {
            log.info("🛒 ORDER SERVICE: Processing discount calculation for order {}", request.getOrderId());

            BigDecimal productDiscount = calculateProductDiscounts(request.getItems());
            log.info("🛒 ORDER SERVICE: Product discount calculated: {}", productDiscount);

            BigDecimal orderDiscount = calculateOrderLevelDiscounts(request);
            log.info("🛒 ORDER SERVICE: Order-level discount calculated: {}", orderDiscount);

            BigDecimal afterOrderDiscount = request.getSubtotal()
                    .subtract(productDiscount)
                    .subtract(orderDiscount);

            if (request.getCouponCodes() != null && !request.getCouponCodes().isEmpty()) {
                log.info("🛒 ORDER SERVICE: Requesting coupon validation for codes: {}", request.getCouponCodes());
                requestCouponValidation(request, afterOrderDiscount, productDiscount, orderDiscount);
            } else {
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

        DiscountCalculationContext context = DiscountCalculationContext.builder()
                .originalRequest(request)
                .productDiscount(productDiscount)
                .orderDiscount(orderDiscount)
                .amountAfterOrderDiscount(afterOrderDiscount)
                .build();

        contexts.put(request.getCorrelationId(), context);

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

        DiscountCalculationContext context = contexts.get(request.getCorrelationId());
        if (context != null) {
            context.setCouponDiscount(couponDiscount);
            context.setAmountAfterCouponDiscount(afterCouponDiscount);
        }

        log.info("🛒 ORDER SERVICE: Sending tier discount request to Loyalty Service");
        kafkaTemplate.send("tier-discount-request",
                request.getCorrelationId(), tierRequest);
    }

    /**
     * Calculate simple order-level discounts without complex rule engine
     * You can implement basic business logic here or remove completely
     */
    private BigDecimal calculateOrderLevelDiscounts(DiscountCalculationRequest request) {
        BigDecimal discount = BigDecimal.ZERO;

        try {
            // Option 1: No order-level discounts
            // return BigDecimal.ZERO;

            // Option 2: Simple hardcoded business rules
            BigDecimal subtotal = request.getSubtotal();
            Integer totalItems = request.getTotalItems();

            // Example: Bulk discount - 10% off for 5+ items
            if (totalItems != null && totalItems >= 5) {
                BigDecimal bulkDiscount = subtotal.multiply(BigDecimal.valueOf(0.10));
                discount = discount.add(bulkDiscount);
                log.info("🛒 ORDER SERVICE: Applied bulk discount (5+ items): {}", bulkDiscount);
            }

            // Example: Minimum purchase discount - $15 off orders over $100
            if (subtotal.compareTo(BigDecimal.valueOf(100)) >= 0) {
                BigDecimal minPurchaseDiscount = BigDecimal.valueOf(15);
                discount = discount.add(minPurchaseDiscount);
                log.info("🛒 ORDER SERVICE: Applied minimum purchase discount ($100+): {}", minPurchaseDiscount);
            }

            // Example: Large order discount - 5% off orders over $500
            if (subtotal.compareTo(BigDecimal.valueOf(500)) >= 0) {
                BigDecimal largeOrderDiscount = subtotal.multiply(BigDecimal.valueOf(0.05));
                // Cap at $50 maximum
                if (largeOrderDiscount.compareTo(BigDecimal.valueOf(50)) > 0) {
                    largeOrderDiscount = BigDecimal.valueOf(50);
                }
                discount = discount.add(largeOrderDiscount);
                log.info("🛒 ORDER SERVICE: Applied large order discount ($500+): {}", largeOrderDiscount);
            }

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error calculating order-level discounts", e);
            // Return zero discount on error to avoid breaking the order
            return BigDecimal.ZERO;
        }

        return discount;
    }

    private BigDecimal calculateProductDiscounts(List<OrderItemResponseDto> items) {
        if (items == null) return BigDecimal.ZERO;

        return items.stream()
                .map(item -> item.getDiscount() != null ? item.getDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void completeWithError(String correlationId, String errorMessage) {
        CompletableFuture<DiscountCalculationResponse> future = pendingCalculations.remove(correlationId);
        contexts.remove(correlationId);

        if (future != null) {
            DiscountCalculationResponse errorResponse = DiscountCalculationResponse.builder()
                    .correlationId(correlationId)
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();

            future.complete(errorResponse);
        }
    }
}