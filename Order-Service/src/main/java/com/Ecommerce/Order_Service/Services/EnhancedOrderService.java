package com.Ecommerce.Order_Service.Services;

import com.Ecommerce.Order_Service.Entities.DiscountApplication;
import com.Ecommerce.Order_Service.Entities.DiscountType;
import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Payload.Kafka.CouponUsageNotification;
import com.Ecommerce.Order_Service.Payload.Kafka.OrderItemDto;
import com.Ecommerce.Order_Service.Payload.Kafka.Request.DiscountCalculationRequest;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountBreakdown;
import com.Ecommerce.Order_Service.Payload.Kafka.Response.DiscountCalculationResponse;
import com.Ecommerce.Order_Service.Repositories.DiscountApplicationRepository;
import com.Ecommerce.Order_Service.Repositories.OrderRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class EnhancedOrderService extends OrderService {
    @Autowired
    private OrderRepository orderRepository;

    private  DiscountCalculationService discountCalculationService;
    private DiscountApplicationRepository discountApplicationRepository;
    private  KafkaTemplate<String, Object> kafkaTemplate;
    private  ObjectMapper objectMapper;


    public Order createOrderWithDiscounts(String userId, UUID cartId,
                                          UUID billingAddressId, UUID shippingAddressId,
                                          List<String> couponCodes) {

        log.info("🛒 ORDER SERVICE: Creating order with discounts for user: {}", userId);

        // Create basic order first
        Order order = super.createOrder(userId, cartId, billingAddressId, shippingAddressId);

        // Calculate initial subtotal
        BigDecimal subtotal = calculateOrderTotal(order.getId());
        order.setTotalAmount(subtotal);

        log.info("🛒 ORDER SERVICE: Order created with ID: {}, subtotal: {}", order.getId(), subtotal);

        // Prepare discount calculation request
        DiscountCalculationRequest discountRequest = DiscountCalculationRequest.builder()
                .correlationId(UUID.randomUUID().toString())
                .userId(UUID.fromString(userId))
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
                    discountFuture.get(10, TimeUnit.SECONDS);

            if (discountResponse.isSuccess()) {
                log.info("🛒 ORDER SERVICE: Discount calculation successful for order: {}", order.getId());

                // Apply discounts to order
                order.setProductDiscount(discountResponse.getProductDiscount());
                order.setOrderLevelDiscount(discountResponse.getOrderLevelDiscount());
                order.setLoyaltyCouponDiscount(discountResponse.getCouponDiscount());
                order.setTierBenefitDiscount(discountResponse.getTierDiscount());
                order.setTotalAmount(discountResponse.getFinalAmount());

                // Calculate total discount
                BigDecimal totalDiscount = discountResponse.getProductDiscount()
                        .add(discountResponse.getOrderLevelDiscount())
                        .add(discountResponse.getCouponDiscount())
                        .add(discountResponse.getTierDiscount());
                order.setDiscount(totalDiscount);

                // Store applied coupon codes and breakdown
                if (couponCodes != null && !couponCodes.isEmpty()) {
                    order.setAppliedCouponCodes(objectMapper.writeValueAsString(couponCodes));
                }

                order.setDiscountBreakdown(objectMapper.writeValueAsString(
                        discountResponse.getBreakdown()));

                Order savedOrder = orderRepository.save(order);

                // Save discount applications for audit
                saveDiscountApplications(savedOrder, discountResponse);

                // Mark coupons as used
                if (couponCodes != null && !couponCodes.isEmpty() &&
                        discountResponse.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    markCouponsAsUsed(couponCodes, order.getId(), UUID.fromString(userId));
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
        List<DiscountApplication> applications = new ArrayList<>();

        for (DiscountBreakdown breakdown : response.getBreakdown()) {
            DiscountApplication application = DiscountApplication.builder()
                    .order(order)
                    .discountType(DiscountType.valueOf(breakdown.getDiscountType()))
                    .discountSource(breakdown.getSource())
                    .originalAmount(response.getOriginalAmount())
                    .discountAmount(breakdown.getAmount())
                    .finalAmount(response.getFinalAmount())
                    .build();

            applications.add(application);
        }

        discountApplicationRepository.saveAll(applications);
        log.info("🛒 ORDER SERVICE: Saved {} discount applications for order {}",
                applications.size(), order.getId());
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
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .discount(item.getDiscount())
                        .build())
                .collect(Collectors.toList());
    }
}
