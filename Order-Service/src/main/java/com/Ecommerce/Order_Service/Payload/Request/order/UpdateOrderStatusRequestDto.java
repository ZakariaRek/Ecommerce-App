package com.Ecommerce.Order_Service.Payload.Request.order;


import com.Ecommerce.Order_Service.Entities.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequestDto {
    @NotNull(message = "Status is required")
    private OrderStatus status;
}