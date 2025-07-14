package com.Ecommerce.Order_Service.Payload.Request.OrderItem;


import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateOrderItemQuantityRequestDto {
    @Positive(message = "Quantity must be positive")
    private int quantity;
}
