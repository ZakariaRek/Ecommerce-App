package com.Ecommerce.Order_Service.Payload.Response.Order;

import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponseDto {
    private UUID id;
    private UUID userId;
    private UUID cartId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal discount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID billingAddressId;
    private UUID shippingAddressId;
    private List<OrderItemResponseDto> items;
}