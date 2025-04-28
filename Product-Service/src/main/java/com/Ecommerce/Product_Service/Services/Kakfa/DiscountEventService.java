package com.Ecommerce.Product_Service.Services.Kakfa;



import com.Ecommerce.Product_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Product_Service.Entities.Discount;
import com.Ecommerce.Product_Service.Events.DiscountEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishDiscountCreatedEvent(Discount discount) {
        try {
            DiscountEvents.DiscountCreatedEvent event = DiscountEvents.DiscountCreatedEvent.builder()
                    .discountId(discount.getId())
                    .productId(discount.getProduct() != null ? discount.getProduct().getId() : null)
                    .productName(discount.getProduct() != null ? discount.getProduct().getName() : null)
                    .discountType(discount.getDiscountType())
                    .discountValue(discount.getDiscountValue())
                    .startDate(discount.getStartDate())
                    .endDate(discount.getEndDate())
                    .minPurchaseAmount(discount.getMinPurchaseAmount())
                    .maxDiscountAmount(discount.getMaxDiscountAmount())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing discount created event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_DISCOUNT_CREATED, discount.getId().toString(), event);
            log.info("Discount created event published successfully for discount ID: {}", discount.getId());
        } catch (Exception e) {
            log.error("Failed to publish discount created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish discount created event", e);
        }
    }

    public void publishDiscountUpdatedEvent(Discount discount) {
        try {
            DiscountEvents.DiscountUpdatedEvent event = DiscountEvents.DiscountUpdatedEvent.builder()
                    .discountId(discount.getId())
                    .productId(discount.getProduct() != null ? discount.getProduct().getId() : null)
                    .productName(discount.getProduct() != null ? discount.getProduct().getName() : null)
                    .discountType(discount.getDiscountType())
                    .discountValue(discount.getDiscountValue())
                    .startDate(discount.getStartDate())
                    .endDate(discount.getEndDate())
                    .minPurchaseAmount(discount.getMinPurchaseAmount())
                    .maxDiscountAmount(discount.getMaxDiscountAmount())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing discount updated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_DISCOUNT_UPDATED, discount.getId().toString(), event);
            log.info("Discount updated event published successfully for discount ID: {}", discount.getId());
        } catch (Exception e) {
            log.error("Failed to publish discount updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish discount updated event", e);
        }
    }

    public void publishDiscountDeletedEvent(Discount discount) {
        try {
            DiscountEvents.DiscountDeletedEvent event = DiscountEvents.DiscountDeletedEvent.builder()
                    .discountId(discount.getId())
                    .productId(discount.getProduct() != null ? discount.getProduct().getId() : null)
                    .productName(discount.getProduct() != null ? discount.getProduct().getName() : null)
                    .discountType(discount.getDiscountType())
                    .discountValue(discount.getDiscountValue())
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing discount deleted event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_DISCOUNT_DELETED, discount.getId().toString(), event);
            log.info("Discount deleted event published successfully for discount ID: {}", discount.getId());
        } catch (Exception e) {
            log.error("Failed to publish discount deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish discount deleted event", e);
        }
    }

    public void publishDiscountActivatedEvent(Discount discount) {
        try {
            BigDecimal originalPrice = discount.getProduct() != null ? discount.getProduct().getPrice() : BigDecimal.ZERO;
            BigDecimal discountedPrice = discount.applyDiscount(originalPrice);

            DiscountEvents.DiscountActivatedEvent event = DiscountEvents.DiscountActivatedEvent.builder()
                    .discountId(discount.getId())
                    .productId(discount.getProduct() != null ? discount.getProduct().getId() : null)
                    .productName(discount.getProduct() != null ? discount.getProduct().getName() : null)
                    .discountType(discount.getDiscountType())
                    .discountValue(discount.getDiscountValue())
                    .originalPrice(originalPrice)
                    .discountedPrice(discountedPrice)
                    .activatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing discount activated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_DISCOUNT_ACTIVATED, discount.getId().toString(), event);
            log.info("Discount activated event published successfully for discount ID: {}", discount.getId());
        } catch (Exception e) {
            log.error("Failed to publish discount activated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish discount activated event", e);
        }
    }

    public void publishDiscountDeactivatedEvent(Discount discount) {
        try {
            DiscountEvents.DiscountDeactivatedEvent event = DiscountEvents.DiscountDeactivatedEvent.builder()
                    .discountId(discount.getId())
                    .productId(discount.getProduct() != null ? discount.getProduct().getId() : null)
                    .productName(discount.getProduct() != null ? discount.getProduct().getName() : null)
                    .discountType(discount.getDiscountType())
                    .discountValue(discount.getDiscountValue())
                    .deactivatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing discount deactivated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_DISCOUNT_DEACTIVATED, discount.getId().toString(), event);
            log.info("Discount deactivated event published successfully for discount ID: {}", discount.getId());
        } catch (Exception e) {
            log.error("Failed to publish discount deactivated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish discount deactivated event", e);
        }
    }

    public void publishDiscountValueChangedEvent(Discount discount, BigDecimal previousDiscountValue) {
        try {
            DiscountEvents.DiscountValueChangedEvent event = DiscountEvents.DiscountValueChangedEvent.builder()
                    .discountId(discount.getId())
                    .productId(discount.getProduct() != null ? discount.getProduct().getId() : null)
                    .productName(discount.getProduct() != null ? discount.getProduct().getName() : null)
                    .discountType(discount.getDiscountType())
                    .previousDiscountValue(previousDiscountValue)
                    .newDiscountValue(discount.getDiscountValue())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing discount value changed event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_DISCOUNT_VALUE_CHANGED, discount.getId().toString(), event);
            log.info("Discount value changed event published successfully for discount ID: {}", discount.getId());
        } catch (Exception e) {
            log.error("Failed to publish discount value changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish discount value changed event", e);
        }
    }

    public void publishDiscountPeriodChangedEvent(Discount discount, LocalDateTime previousStartDate, LocalDateTime previousEndDate) {
        try {
            DiscountEvents.DiscountPeriodChangedEvent event = DiscountEvents.DiscountPeriodChangedEvent.builder()
                    .discountId(discount.getId())
                    .productId(discount.getProduct() != null ? discount.getProduct().getId() : null)
                    .productName(discount.getProduct() != null ? discount.getProduct().getName() : null)
                    .discountType(discount.getDiscountType())
                    .previousStartDate(previousStartDate)
                    .previousEndDate(previousEndDate)
                    .newStartDate(discount.getStartDate())
                    .newEndDate(discount.getEndDate())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing discount period changed event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_DISCOUNT_PERIOD_CHANGED, discount.getId().toString(), event);
            log.info("Discount period changed event published successfully for discount ID: {}", discount.getId());
        } catch (Exception e) {
            log.error("Failed to publish discount period changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish discount period changed event", e);
        }
    }
}