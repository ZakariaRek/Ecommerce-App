package com.Ecommerce.Order_Service.Controllers;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;

import com.Ecommerce.Order_Service.Payload.OrderMapper;
import com.Ecommerce.Order_Service.Payload.Request.OrderItem.CreateOrderItemRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.OrderItem.UpdateOrderItemQuantityRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.order.CreateOrderRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.order.UpdateOrderStatusRequestDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.InvoiceResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderTotalResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import com.Ecommerce.Order_Service.Services.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for order operations using DTOs
 */
@RestController
@RequestMapping("/order")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;


    /**
     * Get an orders list
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        try {
            List<Order> orders = orderService.getAllOrders();
            List<OrderResponseDto> responseDtos = orderMapper.toOrderResponseDtoList(orders);
            return ResponseEntity.ok(responseDtos);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving orders: " + e.getMessage());
        }
    }

    /**
     * Create a new order
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody CreateOrderRequestDto orderRequest) {
        try {
            Order newOrder = orderService.createOrder(
                    orderRequest.getUserId(),
                    orderRequest.getCartId(),
                    orderRequest.getBillingAddressId(),
                    orderRequest.getShippingAddressId()
            );

            // Add items if provided in the request
            if (orderRequest.getItems() != null && !orderRequest.getItems().isEmpty()) {
                List<OrderItem> items = orderMapper.toOrderItemList(orderRequest.getItems());
                for (OrderItem item : items) {
                    orderService.addOrderItem(newOrder.getId(), item);
                }
                // Refresh the order to get updated items and total
                newOrder = orderService.getOrderById(newOrder.getId());
            }

            OrderResponseDto responseDto = orderMapper.toOrderResponseDto(newOrder);
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating order: " + e.getMessage());
        }
    }

    /**
     * Get an order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable UUID orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            OrderResponseDto responseDto = orderMapper.toOrderResponseDto(order);
            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Get all orders for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByUser(@PathVariable UUID userId) {
        List<Order> orders = orderService.getOrdersByUserId(userId);
        List<OrderResponseDto> responseDtos = orderMapper.toOrderResponseDtoList(orders);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Update order status
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequestDto statusUpdate) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, statusUpdate.getStatus());
            OrderResponseDto responseDto = orderMapper.toOrderResponseDto(updatedOrder);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Cancel an order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable UUID orderId) {
        try {
            Order canceledOrder = orderService.cancelOrder(orderId);
            OrderResponseDto responseDto = orderMapper.toOrderResponseDto(canceledOrder);
            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Generate invoice for an order
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<InvoiceResponseDto> generateInvoice(@PathVariable UUID orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            String invoiceData = orderService.generateInvoice(orderId);
            InvoiceResponseDto responseDto = orderMapper.toInvoiceResponseDto(invoiceData, order);
            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Get order items for an order
     */
    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItemResponseDto>> getOrderItems(@PathVariable UUID orderId) {
        try {
            List<OrderItem> items = orderService.getOrderItems(orderId);
            List<OrderItemResponseDto> responseDtos = orderMapper.toOrderItemResponseDtoList(items);
            return ResponseEntity.ok(responseDtos);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Add item to an order
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItemResponseDto> addOrderItem(
            @PathVariable UUID orderId,
            @Valid @RequestBody CreateOrderItemRequestDto orderItemRequest) {
        try {
            OrderItem orderItemEntity = orderMapper.toOrderItem(orderItemRequest);
            OrderItem addedItem = orderService.addOrderItem(orderId, orderItemEntity);
            OrderItemResponseDto responseDto = orderMapper.toOrderItemResponseDto(addedItem);
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Update item quantity
     */
    @PatchMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderItemResponseDto> updateItemQuantity(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateOrderItemQuantityRequestDto quantityUpdate) {
        try {
            OrderItem updatedItem = orderService.updateOrderItemQuantity(
                    orderId,
                    itemId,
                    quantityUpdate.getQuantity()
            );
            OrderItemResponseDto responseDto = orderMapper.toOrderItemResponseDto(updatedItem);
            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Calculate order total
     */
    @GetMapping("/{orderId}/total")
    public ResponseEntity<OrderTotalResponseDto> calculateOrderTotal(@PathVariable UUID orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            BigDecimal total = orderService.calculateOrderTotal(orderId);
            OrderTotalResponseDto responseDto = orderMapper.toOrderTotalResponseDto(total, order);
            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Delete/Remove an order item
     */
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<Void> removeOrderItem(
            @PathVariable UUID orderId,
            @PathVariable UUID itemId) {
        try {
            orderService.removeOrderItem(orderId, itemId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Get order summary (without items details)
     */
    @GetMapping("/{orderId}/summary")
    public ResponseEntity<OrderResponseDto> getOrderSummary(@PathVariable UUID orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            OrderResponseDto responseDto = orderMapper.toOrderResponseDto(order);
            // Clear items for summary view
            responseDto.setItems(null);
            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}