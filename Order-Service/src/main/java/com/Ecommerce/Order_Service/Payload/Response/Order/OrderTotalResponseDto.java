package com.Ecommerce.Order_Service.Payload.Response.Order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderTotalResponseDto {
    private BigDecimal total;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal discount;
}