package com.Ecommerce.Loyalty_Service.Listeners.AsnycComm;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Entities.CouponUsageHistory;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.CouponUsageNotification;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Request.CouponValidationRequest;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Request.TierDiscountRequest;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.CouponValidationResponse;
import com.Ecommerce.Loyalty_Service.Payload.Kafka.Response.TierDiscountResponse;
import com.Ecommerce.Loyalty_Service.Repositories.CouponRepository;
import com.Ecommerce.Loyalty_Service.Repositories.CouponUsageHistoryRepository;
import com.Ecommerce.Loyalty_Service.Services.CouponValidationService;
import com.Ecommerce.Loyalty_Service.Services.TierDiscountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoyaltyDiscountKafkaListener {

    private final TierDiscountService tierDiscountService;
    private final CouponValidationService couponValidationService;
    private final CouponRepository couponRepository;
    private final CouponUsageHistoryRepository usageHistoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "tier-discount-request", groupId = "loyalty-service-group")
    public void handleTierDiscountRequest(ConsumerRecord<String, Object> record) {
        try {
            TierDiscountRequest request = objectMapper.convertValue(
                    record.value(), TierDiscountRequest.class);

            log.info("💎 LOYALTY SERVICE: Received tier discount request for user {} with correlation {}",
                    request.getUserId(), request.getCorrelationId());

            TierDiscountResponse response = tierDiscountService
                    .calculateTierDiscount(request.getUserId(), request.getAmount());
            response.setCorrelationId(request.getCorrelationId());

            log.info("💎 LOYALTY SERVICE: Sending tier discount response: {}", response.getDiscountAmount());
            kafkaTemplate.send("tier-discount-response",
                    request.getCorrelationId(), response);

        } catch (Exception e) {
            log.error("💎 LOYALTY SERVICE: Error processing tier discount request", e);
        }
    }

    @KafkaListener(topics = "coupon-validation-request", groupId = "loyalty-service-group")
    public void handleCouponValidationRequest(ConsumerRecord<String, Object> record) {
        try {
            CouponValidationRequest request = objectMapper.convertValue(
                    record.value(), CouponValidationRequest.class);

            log.info("💳 LOYALTY SERVICE: Received coupon validation request for codes {} with correlation {}",
                    request.getCouponCodes(), request.getCorrelationId());

            CouponValidationResponse response = couponValidationService
                    .validateAndCalculateDiscount(
                            request.getCouponCodes(),
                            request.getUserId(),
                            request.getAmount()
                    );
            response.setCorrelationId(request.getCorrelationId());

            log.info("💳 LOYALTY SERVICE: Sending coupon validation response: success={}, discount={}",
                    response.isSuccess(), response.getTotalDiscount());
            kafkaTemplate.send("coupon-validation-response",
                    request.getCorrelationId(), response);

        } catch (Exception e) {
            log.error("💳 LOYALTY SERVICE: Error processing coupon validation request", e);
        }
    }

    @KafkaListener(topics = "coupon-usage-notification", groupId = "loyalty-service-group")
    public void handleCouponUsageNotification(ConsumerRecord<String, Object> record) {
        try {
            CouponUsageNotification notification = objectMapper.convertValue(
                    record.value(), CouponUsageNotification.class);

            log.info("💳 LOYALTY SERVICE: Received coupon usage notification for codes: {}",
                    notification.getCouponCodes());

            for (String couponCode : notification.getCouponCodes()) {
                markCouponAsUsed(couponCode, notification.getUserId(), notification.getOrderId());
            }

        } catch (Exception e) {
            log.error("💳 LOYALTY SERVICE: Error processing coupon usage notification", e);
        }
    }

    private void markCouponAsUsed(String couponCode, UUID userId, UUID orderId) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);
            if (couponOpt.isEmpty()) {
                log.warn("💳 LOYALTY SERVICE: Coupon not found for usage marking: {}", couponCode);
                return;
            }

            Coupon coupon = couponOpt.get();

            // Mark coupon as used (for single-use coupons)
            if (coupon.getUsageLimit() == 1) {
                coupon.setUsed(true);
            }

            // Increment usage count
            couponRepository.save(coupon);

            // Create usage history record
            CouponUsageHistory usageHistory = CouponUsageHistory.builder()
                    .coupon(coupon)
                    .userId(userId)
                    .orderId(orderId)
                    .discountAmount(BigDecimal.ZERO) // Will be updated later
                    .build();

            usageHistoryRepository.save(usageHistory);

            log.info("💳 LOYALTY SERVICE: Marked coupon {} as used for user {}", couponCode, userId);

        } catch (Exception e) {
            log.error("💳 LOYALTY SERVICE: Error marking coupon {} as used: {}", couponCode, e.getMessage());
        }
    }
}