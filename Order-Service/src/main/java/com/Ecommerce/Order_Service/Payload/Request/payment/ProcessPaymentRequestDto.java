package com.Ecommerce.Order_Service.Payload.Request.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequestDto {
    @NotNull(message = "Order ID is required")
    private String orderId;  // Matches Go struct field name

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Payment method is required")
    private String paymentMethod;  // Matches Go struct field name

    private String currency = "USD";
}