package com.Ecommerce.Order_Service.Payload;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Payload.Request.OrderItem.CreateOrderItemRequestDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.InvoiceResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderTotalResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponseDto toOrderResponseDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setCartId(order.getCartId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setTax(order.getTax());
        dto.setShippingCost(order.getShippingCost());
        dto.setDiscount(order.getDiscount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setBillingAddressId(order.getBillingAddressId());
        dto.setShippingAddressId(order.getShippingAddressId());

        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream()
                    .map(this::toOrderItemResponseDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public OrderItemResponseDto toOrderItemResponseDto(OrderItem item) {
        OrderItemResponseDto dto = new OrderItemResponseDto();
        dto.setId(item.getId());
        dto.setProductId(item.getProductId());
        dto.setQuantity(item.getQuantity());
        dto.setPriceAtPurchase(item.getPriceAtPurchase());
        dto.setDiscount(item.getDiscount());
        dto.setTotal(item.getTotal());
        return dto;
    }

    public OrderItem toOrderItem(CreateOrderItemRequestDto dto) {
        OrderItem item = new OrderItem();
        item.setProductId(dto.getProductId());
        item.setQuantity(dto.getQuantity());
        item.setPriceAtPurchase(dto.getPriceAtPurchase());
        item.setDiscount(dto.getDiscount());
        return item;
    }

    public List<OrderItem> toOrderItemList(List<CreateOrderItemRequestDto> itemDtos) {
        if (itemDtos == null) {
            return null;
        }
        return itemDtos.stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList());
    }

    public OrderTotalResponseDto toOrderTotalResponseDto(BigDecimal total, Order order) {
        OrderTotalResponseDto dto = new OrderTotalResponseDto();
        dto.setTotal(total);
        dto.setSubtotal(total.subtract(order.getTax()).subtract(order.getShippingCost()).add(order.getDiscount()));
        dto.setTax(order.getTax());
        dto.setShippingCost(order.getShippingCost());
        dto.setDiscount(order.getDiscount());
        return dto;
    }

    public InvoiceResponseDto toInvoiceResponseDto(String invoiceData, Order order) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setOrderId(order.getId());
        dto.setInvoiceData(invoiceData);
        dto.setDownloadUrl("/api/orders/" + order.getId() + "/invoice/download");
        return dto;
    }

    public List<OrderResponseDto> toOrderResponseDtoList(List<Order> orders) {
        return orders.stream()
                .map(this::toOrderResponseDto)
                .collect(Collectors.toList());
    }

    public List<OrderItemResponseDto> toOrderItemResponseDtoList(List<OrderItem> items) {
        return items.stream()
                .map(this::toOrderItemResponseDto)
                .collect(Collectors.toList());
    }
}