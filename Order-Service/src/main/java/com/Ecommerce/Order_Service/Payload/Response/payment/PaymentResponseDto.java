
// Order-Service/src/main/java/com/Ecommerce/Order_Service/Payload/Response/payment/PaymentResponseDto.java
package com.Ecommerce.Order_Service.Payload.Response.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponseDto {
    private String paymentId;
    private String orderId;
    private String transactionId;
    private String status;
    private boolean success;
    private String paymentMethod;
    private BigDecimal amount;
    private String message;
    private String createdAt; // Add this field if needed
}