
// Order-Service/src/main/java/com/Ecommerce/Order_Service/Payload/Response/payment/PaymentResponseDto.java
package com.Ecommerce.Order_Service.Payload.Response.payment;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private String paymentId;     // Matches Go response field name
    private String orderId;       // Matches Go response field name
    private Double amount;
    private String status;
    private String paymentMethod; // Matches Go response field name
    private String transactionId; // Matches Go response field name
    private LocalDateTime createdAt; // Matches Go response field name
    private String message;
    private Boolean success;      // Matches Go response field name
}