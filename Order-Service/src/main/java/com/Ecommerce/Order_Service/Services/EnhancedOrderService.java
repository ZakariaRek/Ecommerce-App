// Fixed EnhancedOrderService.java
package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.DiscountApplication;
import com.Ecommerce.Order_Service.Entities.DiscountType;
import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Payload.Kafka.CouponUsageNotification;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountBreakdown;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountCalculationResponse;
import com.Ecommerce.Order_Service.Repositories.DiscountApplicationRepository;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
import com.Ecommerce.Order_Service.KafkaProducers.DiscountCalculationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class EnhancedOrderService extends OrderService {

    private final DiscountCalculationService discountCalculationService;
    private final DiscountApplicationRepository discountApplicationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    // Proper constructor injection
    public EnhancedOrderService(
            DiscountCalculationService discountCalculationService,
            DiscountApplicationRepository discountApplicationRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.discountCalculationService = discountCalculationService;
        this.discountApplicationRepository = discountApplicationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Order createOrderWithDiscounts(String userId, UUID cartId,
                                          UUID billingAddressId, UUID shippingAddressId,
                                          List<String> couponCodes) {

        log.info("🛒 ORDER SERVICE: Creating order with discounts for user: {}, coupon codes: {}", userId, couponCodes);

        // Create basic order first
        Order order = super.createOrder(userId, cartId, billingAddressId, shippingAddressId);

        // ALWAYS store coupon codes in the order, even if no items yet
        if (couponCodes != null && !couponCodes.isEmpty()) {
            try {
                order.setAppliedCouponCodes(objectMapper.writeValueAsString(couponCodes));
                order = orderRepository.save(order);
                log.info("🛒 ORDER SERVICE: Stored coupon codes {} in order {}", couponCodes, order.getId());
            } catch (JsonProcessingException e) {
                log.error("🛒 ORDER SERVICE: Failed to serialize coupon codes", e);
            }
        }

        // If no items yet, return order with stored coupon codes (items will be added separately)
        if (order.getItems() == null || order.getItems().isEmpty()) {
            order.setTotalAmount(BigDecimal.ZERO);
            log.info("🛒 ORDER SERVICE: Order created without items, coupon codes stored for later application");
            return order;
        }

        // If items exist, apply discounts immediately
        return applyDiscountsToOrder(order, couponCodes);
    }

    @Transactional
    public Order applyDiscountsToOrder(Order order, List<String> couponCodes) {
        // Calculate initial subtotal
        BigDecimal subtotal = calculateOrderTotal(order.getId());
        order.setTotalAmount(subtotal);

        log.info("🛒 ORDER SERVICE: Applying discounts to order ID: {}, subtotal: {}, coupon codes: {}, coupon codes null: {}",
                order.getId(), subtotal, couponCodes, couponCodes == null);

        // If subtotal is zero, no discounts to apply
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            return orderRepository.save(order);
        }

        // Prepare discount calculation request
        DiscountCalculationRequest discountRequest = DiscountCalculationRequest.builder()
                .correlationId(UUID.randomUUID().toString())
                .userId(UUID.fromString(order.getUserId().toString()))
                .orderId(order.getId())
                .subtotal(subtotal)
                .totalItems(order.getItems().size())
                .couponCodes(couponCodes)
                .items(convertToOrderItemDtos(order.getItems()))
                .build();

        try {
            // Calculate discounts asynchronously
            CompletableFuture<DiscountCalculationResponse> discountFuture =
                    discountCalculationService.calculateOrderDiscounts(discountRequest);

            // Wait for discount calculation (with timeout)
            DiscountCalculationResponse discountResponse =
                    discountFuture.get(15, TimeUnit.SECONDS); // Increased timeout

            if (discountResponse.isSuccess()) {
                log.info("🛒 ORDER SERVICE: Discount calculation successful for order: {}", order.getId());

                // Apply discounts to order
                order.setProductDiscount(discountResponse.getProductDiscount() != null ?
                        discountResponse.getProductDiscount() : BigDecimal.ZERO);
                order.setOrderLevelDiscount(discountResponse.getOrderLevelDiscount() != null ?
                        discountResponse.getOrderLevelDiscount() : BigDecimal.ZERO);
                order.setLoyaltyCouponDiscount(discountResponse.getCouponDiscount() != null ?
                        discountResponse.getCouponDiscount() : BigDecimal.ZERO);
                order.setTierBenefitDiscount(discountResponse.getTierDiscount() != null ?
                        discountResponse.getTierDiscount() : BigDecimal.ZERO);
                order.setTotalAmount(discountResponse.getFinalAmount() != null ?
                        discountResponse.getFinalAmount() : subtotal);

                // Calculate total discount
                BigDecimal totalDiscount = order.getProductDiscount()
                        .add(order.getOrderLevelDiscount())
                        .add(order.getLoyaltyCouponDiscount())
                        .add(order.getTierBenefitDiscount());
                order.setDiscount(totalDiscount);

                // Store applied coupon codes and breakdown
                if (couponCodes != null && !couponCodes.isEmpty()) {
                    order.setAppliedCouponCodes(objectMapper.writeValueAsString(couponCodes));
                }

                if (discountResponse.getBreakdown() != null) {
                    order.setDiscountBreakdown(objectMapper.writeValueAsString(
                            discountResponse.getBreakdown()));
                }

                Order savedOrder = orderRepository.save(order);

                // Save discount applications for audit
                saveDiscountApplications(savedOrder, discountResponse);

                // Mark coupons as used
                if (couponCodes != null && !couponCodes.isEmpty() &&
                        order.getLoyaltyCouponDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    markCouponsAsUsed(couponCodes, order.getId(), order.getUserId());
                }

                log.info("🛒 ORDER SERVICE: Order saved with final amount: {} (discount: {})",
                        savedOrder.getTotalAmount(), totalDiscount);

                return savedOrder;
            } else {
                log.warn("🛒 ORDER SERVICE: Discount calculation failed: {}", discountResponse.getErrorMessage());
                return orderRepository.save(order);
            }

        } catch (TimeoutException e) {
            log.error("🛒 ORDER SERVICE: Discount calculation timeout for order {}", order.getId());
            return orderRepository.save(order);
        } catch (Exception e) {
            log.error("🛒 ORDER SERVICE: Error calculating discounts for order {}: {}", order.getId(), e.getMessage());
            return orderRepository.save(order);
        }
    }

    private void saveDiscountApplications(Order order, DiscountCalculationResponse response) throws JsonProcessingException {
        if (response.getBreakdown() == null || response.getBreakdown().isEmpty()) {
            return;
        }

        List<DiscountApplication> applications = new ArrayList<>();

        for (DiscountBreakdown breakdown : response.getBreakdown()) {
            try {
                DiscountApplication application = DiscountApplication.builder()
                        .order(order)
                        .discountType(DiscountType.valueOf(breakdown.getDiscountType()))
                        .discountSource(breakdown.getSource())
                        .originalAmount(response.getOriginalAmount())
                        .discountAmount(breakdown.getAmount())
                        .finalAmount(response.getFinalAmount())
                        .build();

                applications.add(application);
            } catch (IllegalArgumentException e) {
                log.warn("🛒 ORDER SERVICE: Unknown discount type: {}", breakdown.getDiscountType());
            }
        }

        if (!applications.isEmpty()) {
            discountApplicationRepository.saveAll(applications);
            log.info("🛒 ORDER SERVICE: Saved {} discount applications for order {}",
                    applications.size(), order.getId());
        }
    }

    private void markCouponsAsUsed(List<String> couponCodes, UUID orderId, UUID userId) {
        CouponUsageNotification notification = CouponUsageNotification.builder()
                .couponCodes(couponCodes)
                .orderId(orderId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        log.info("🛒 ORDER SERVICE: Sending coupon usage notification for codes: {}", couponCodes);
        kafkaTemplate.send("coupon-usage-notification", notification);
    }

    private List<OrderItemResponseDto> convertToOrderItemDtos(List<OrderItem> items) {
        return items.stream()
                .map(item -> OrderItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .discount(item.getDiscount())
                        .total(item.getTotal())
                        .build())
                .collect(Collectors.toList());
    }

    // Override addOrderItem to trigger discount recalculation when first item is added
    @Override
    public OrderItem addOrderItem(UUID orderId, OrderItem orderItem) {
        OrderItem addedItem = super.addOrderItem(orderId, orderItem);

        // Get the updated order with all items
        Order order = getOrderById(orderId);

        log.info("🛒 ORDER SERVICE: Added item to order {}, total items now: {}", orderId, order.getItems().size());

        // If this was the first item added and we have stored coupon codes, apply discounts
        if (order.getItems().size() == 1) {
            List<String> couponCodes = getStoredCouponCodes(order);
            if (couponCodes != null && !couponCodes.isEmpty()) {
                log.info("🛒 ORDER SERVICE: First item added, applying stored coupon codes: {}", couponCodes);
                applyDiscountsToOrder(order, couponCodes);
            } else {
                log.info("🛒 ORDER SERVICE: First item added, no coupon codes to apply");
                // Still apply order-level discounts even without coupons
                applyDiscountsToOrder(order, null);
            }
        } else if (order.getItems().size() > 1) {
            // Recalculate discounts for additional items
            List<String> couponCodes = getStoredCouponCodes(order);
            log.info("🛒 ORDER SERVICE: Additional item added, recalculating discounts with coupon codes: {}", couponCodes);
            applyDiscountsToOrder(order, couponCodes);
        }

        return addedItem;
    }

    /**
     * Helper method to retrieve stored coupon codes from order
     */
    private List<String> getStoredCouponCodes(Order order) {
        try {
            if (order.getAppliedCouponCodes() != null && !order.getAppliedCouponCodes().isEmpty()) {
                TypeReference<List<String>> typeRef = new TypeReference<List<String>>() {};
                List<String> couponCodes = objectMapper.readValue(order.getAppliedCouponCodes(), typeRef);
                log.debug("🛒 ORDER SERVICE: Retrieved stored coupon codes: {}", couponCodes);
                return couponCodes;
            }
        } catch (Exception e) {
            log.warn("🛒 ORDER SERVICE: Could not parse coupon codes from order: {}", e.getMessage());
        }
        return null;
    }
}