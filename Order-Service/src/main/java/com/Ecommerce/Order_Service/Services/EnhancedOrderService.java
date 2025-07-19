// Replace the existing EnhancedOrderService.java with this fixed version:

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
import com.Ecommerce.Order_Service.Services.Kafka.DiscountCalculationService;
import com.fasterxml.jackson.core.JsonProcessingException;
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

        log.info("🛒 ORDER SERVICE: Creating order with discounts for user: {}", userId);

        // Create basic order first
        Order order = super.createOrder(userId, cartId, billingAddressId, shippingAddressId);

        // If no items yet, set basic totals and return (items will be added separately)
        if (order.getItems() == null || order.getItems().isEmpty()) {
            order.setTotalAmount(BigDecimal.ZERO);
            return orderRepository.save(order);
        }

        return applyDiscountsToOrder(order, couponCodes);
    }

    @Transactional
    public Order applyDiscountsToOrder(Order order, List<String> couponCodes) {
        // Calculate initial subtotal
        BigDecimal subtotal = calculateOrderTotal(order.getId());
        order.setTotalAmount(subtotal);

        log.info("🛒 ORDER SERVICE: Applying discounts to order ID: {}, subtotal: {}", order.getId(), subtotal);

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

    // Override addOrderItem to trigger discount recalculation
    @Override
    public OrderItem addOrderItem(UUID orderId, OrderItem orderItem) {
        OrderItem addedItem = super.addOrderItem(orderId, orderItem);

        // If this was the first item added, apply discounts
        Order order = getOrderById(orderId);
        if (order.getItems().size() == 1) {
            // Try to get coupon codes from order if they were stored
            List<String> couponCodes = null;
            try {
                if (order.getAppliedCouponCodes() != null && !order.getAppliedCouponCodes().isEmpty()) {
                    couponCodes = objectMapper.readValue(order.getAppliedCouponCodes(), List.class);
                }
            } catch (Exception e) {
                log.warn("🛒 ORDER SERVICE: Could not parse coupon codes from order");
            }

            applyDiscountsToOrder(order, couponCodes);
        }

        return addedItem;
    }
}