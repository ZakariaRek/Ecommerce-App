package com.Ecommerce.Order_Service.Payload.Request.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequestDto {
    @NotNull(message = "Payment method is required")
    private String paymentMethod;

    private String currency = "USD";
}