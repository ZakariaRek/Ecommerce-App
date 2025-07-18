package com.Ecommerce.Order_Service.Payload.Kafka.Request;
import com.Ecommerce.Order_Service.Payload.Request.OrderItem.CreateOrderItemRequestDto;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCalculationRequest {
    private String correlationId;
    private UUID userId;
    private UUID orderId;
    private BigDecimal subtotal;
    private Integer totalItems;
    private List<String> couponCodes;
    private List<OrderItemResponseDto> items;
    private Map<String, Object> additionalData;
}
