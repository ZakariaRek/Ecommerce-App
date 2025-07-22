
// Order-Service/src/main/java/com/Ecommerce/Order_Service/Payload/Response/payment/PaymentResponseDto.java
package com.Ecommerce.Order_Service.Payload.Response.payment;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private String paymentId;
    private String orderId;
    private Double amount;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime createdAt;
    private String message;
    private Boolean success;
}