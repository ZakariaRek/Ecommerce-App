package com.Ecommerce.Product_Service.Listener;


import com.Ecommerce.Product_Service.Entities.Discount;
import com.Ecommerce.Product_Service.Services.Kakfa.DiscountEventService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DiscountEntityListener {

    private static DiscountEventService discountEventService;

    @Autowired
    public void setDiscountEventService(DiscountEventService discountEventService) {
        DiscountEntityListener.discountEventService = discountEventService;
    }

    @PostPersist
    public void postPersist(Discount discount) {
        if (discountEventService != null) {
            discountEventService.publishDiscountCreatedEvent(discount);

            // If the discount is already active at creation time, also publish an activation event
            if (discount.isActive()) {
                discountEventService.publishDiscountActivatedEvent(discount);
            }
        }
    }

    @PostUpdate
    public void postUpdate(Discount discount) {
        if (discountEventService != null) {
            // Basic update event
            discountEventService.publishDiscountUpdatedEvent(discount);

            // The @PostUpdate doesn't provide the previous state
            // In a real application, you would need to use @PreUpdate to capture previous values
            // and store them in thread-local variables or a similar mechanism
            // Then check if specific properties changed and publish specialized events
        }
    }

    @PostRemove
    public void postRemove(Discount discount) {
        if (discountEventService != null) {
            discountEventService.publishDiscountDeletedEvent(discount);
        }
    }

    @PreUpdate
    public void preUpdate(Discount discount) {
        // In a real application, you would store the current state before the update
        // For example, using a ThreadLocal variable or similar mechanism
        // ThreadLocalContext.setPreviousDiscountValue(discount.getDiscountValue());
        // ThreadLocalContext.setPreviousStartDate(discount.getStartDate());
        // ThreadLocalContext.setPreviousEndDate(discount.getEndDate());

        // Also capture the previous active state to detect activation/deactivation
        // ThreadLocalContext.setPreviousActiveState(discount.isActive());
    }
}