package com.Ecommerce.Order_Service.Payload.Request.OrderItem;



import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateOrderItemRequestDto {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @Positive(message = "Quantity must be positive")
    private int quantity;

    @NotNull(message = "Price at purchase is required")
    @Positive(message = "Price must be positive")
    private BigDecimal priceAtPurchase;

    private BigDecimal discount = BigDecimal.ZERO;
}