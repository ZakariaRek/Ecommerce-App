// Fixed Order-Service: DiscountCalculationService.java
package com.Ecommerce.Order_Service.KafkaProducers;

import com.Ecommerce.Order_Service.Payload.Kafka.DiscountCalculationContext;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.CombinedDiscountRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
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
            log.info("🛒 ORDER SERVICE: Processing optimized discount calculation for order {}", request.getOrderId());

            // Step 1: Calculate product-level discounts
            BigDecimal productDiscount = calculateProductDiscounts(request.getItems());
            log.info("🛒 ORDER SERVICE: Product discount calculated: {}", productDiscount);
            log.info("🛒 ORDER SERVICE: cOUPONS : {}", request.getCouponCodes());

            // Step 2: Calculate order-level discounts (bulk, minimum purchase, etc.)
            BigDecimal orderDiscount = calculateOrderLevelDiscounts(request);
            log.info("🛒 ORDER SERVICE: Order-level discount calculated: {}", orderDiscount);

            // Step 3: Calculate amount after order discounts (base for coupon + tier calculations)
            BigDecimal afterOrderDiscount = request.getSubtotal()
                    .subtract(productDiscount)
                    .subtract(orderDiscount);

            log.info("🛒 ORDER SERVICE: Amount after order discounts: {}", afterOrderDiscount);

            // Step 4: Send single combined request for both coupon and tier discounts
            requestCombinedLoyaltyDiscounts(request, afterOrderDiscount, productDiscount, orderDiscount);

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error in optimized discount calculation process", e);
            completeWithError(request.getCorrelationId(), "Discount calculation failed: " + e.getMessage());
        }
    }

    private void requestCombinedLoyaltyDiscounts(DiscountCalculationRequest originalRequest,
                                                 BigDecimal afterOrderDiscount,
                                                 BigDecimal productDiscount,
                                                 BigDecimal orderDiscount) {
        try {
            log.info("🛒 ORDER SERVICE: Original request coupon codes: {}", originalRequest.getCouponCodes());
            log.info("🛒 ORDER SERVICE: Building combined request for order: {}", originalRequest.getOrderId());

            // Create combined request that includes both coupon and tier discount requirements
            CombinedDiscountRequest combinedRequest = CombinedDiscountRequest.builder()
                    .correlationId(originalRequest.getCorrelationId())
                    .userId(originalRequest.getUserId())
                    .orderId(originalRequest.getOrderId())
                    .originalAmount(originalRequest.getSubtotal())
                    .productDiscount(productDiscount)
                    .orderLevelDiscount(orderDiscount)
                    .amountAfterOrderDiscounts(afterOrderDiscount)
                    .couponCodes(originalRequest.getCouponCodes()) // Can be null or empty
                    .totalItems(originalRequest.getTotalItems())
                    .build();

            log.info("🛒 ORDER SERVICE: Combined request coupon codes: {}", combinedRequest.getCouponCodes());

            // FIX: Store context for response processing
            DiscountCalculationContext context = DiscountCalculationContext.builder()
                    .originalRequest(originalRequest)
                    .productDiscount(productDiscount)
                    .orderDiscount(orderDiscount)
                    .amountAfterOrderDiscount(afterOrderDiscount)
                    .build();

            // FIX: Store the context properly
            contexts.put(originalRequest.getCorrelationId(), context);
            log.info("🛒 ORDER SERVICE: Stored context for correlation: {}", originalRequest.getCorrelationId());

            // Send single request to Loyalty Service
            log.info("🛒 ORDER SERVICE: Sending combined discount request for order {} with coupons: {} and amount: {}",
                    originalRequest.getOrderId(),
                    originalRequest.getCouponCodes(),
                    afterOrderDiscount);

            kafkaTemplate.send("combined-discount-request",
                    originalRequest.getCorrelationId(),
                    combinedRequest);

            log.info("🛒 ORDER SERVICE: Combined discount request sent successfully");

        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error sending combined discount request", e);
            completeWithError(originalRequest.getCorrelationId(),
                    "Failed to request combined discounts: " + e.getMessage());
        }
    }

    /**
     * Calculate simple order-level discounts without complex rule engine
     */
    private BigDecimal calculateOrderLevelDiscounts(DiscountCalculationRequest request) {
        BigDecimal discount = BigDecimal.ZERO;

        try {
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

    /**
     * FIX: Add public method to complete pending calculations from response listener
     */
    public void completePendingCalculation(String correlationId, DiscountCalculationResponse response) {
        CompletableFuture<DiscountCalculationResponse> future = pendingCalculations.remove(correlationId);
        contexts.remove(correlationId);

        if (future != null) {
            future.complete(response);
            log.info("🛒 ORDER SERVICE: Completed pending calculation for correlation: {}", correlationId);
        } else {
            log.warn("🛒 ORDER SERVICE: No pending calculation found for correlation: {}", correlationId);
        }
    }

    /**
     * FIX: Add public method to get context from response listener
     */
    public DiscountCalculationContext getContext(String correlationId) {
        return contexts.get(correlationId);
    }

    /**
     * FIX: Add debugging method to see available context keys
     */
    public java.util.Set<String> getAvailableContextKeys() {
        return contexts.keySet();
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