package com.Ecommerce.Gateway_Service.DTOs.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * ✅ Helper class for batch processing results
 */
@Data
@Builder
@AllArgsConstructor
public  class OrderResult {
    private String orderId;
    private EnrichedOrderResponse order;
    private String errorMessage;
    private boolean success;

    public static OrderResult success(String orderId, EnrichedOrderResponse order) {
        return OrderResult.builder()
                .orderId(orderId)
                .order(order)
                .success(true)
                .build();
    }

    public static OrderResult failure(String orderId, String errorMessage) {
        return OrderResult.builder()
                .orderId(orderId)
                .errorMessage(errorMessage)
                .success(false)
                .build();
    }
}
