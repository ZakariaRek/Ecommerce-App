package com.Ecommerce.Order_Service.Payload.Request.order;

import com.Ecommerce.Order_Service.Payload.Request.OrderItem.CreateOrderItemRequestDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderWithDiscountsRequestDto {
    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Cart ID is required")
    private UUID cartId;

    @NotNull(message = "Billing address ID is required")
    private UUID billingAddressId;

    @NotNull(message = "Shipping address ID is required")
    private UUID shippingAddressId;

    private List<CreateOrderItemRequestDto> items;

    private List<String> couponCodes; // This is the key addition for discount functionality
}