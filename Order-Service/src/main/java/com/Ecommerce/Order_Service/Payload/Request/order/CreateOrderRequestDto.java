package com.Ecommerce.Order_Service.Payload.Request.order;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateOrderRequestDto {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Cart ID is required")
    private UUID cartId;

    @NotNull(message = "Billing address ID is required")
    private UUID billingAddressId;

    @NotNull(message = "Shipping address ID is required")
    private UUID shippingAddressId;
}
