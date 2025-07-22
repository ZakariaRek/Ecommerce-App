package com.Ecommerce.Order_Service.Controllers;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;

import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Payload.OrderMapper;
import com.Ecommerce.Order_Service.Payload.Request.OrderItem.CreateOrderItemRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.OrderItem.UpdateOrderItemQuantityRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.order.CreateOrderRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.order.CreateOrderWithDiscountsRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.order.UpdateOrderStatusRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.payment.PaymentMethodRequestDto;
import com.Ecommerce.Order_Service.Payload.Request.payment.ProcessPaymentRequestDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.InvoiceResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderTotalResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.payment.PaymentResponseDto;
import com.Ecommerce.Order_Service.Services.EnhancedOrderService;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.Ecommerce.Order_Service.Services.PaymentIntegrationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for order operations using DTOs
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private EnhancedOrderService enhancedOrderService;



    @PostMapping("/with-discounts")
    public ResponseEntity<OrderResponseDto> createOrderWithDiscounts(
            @Valid @RequestBody CreateOrderWithDiscountsRequestDto orderRequest) {
        try {
            Order newOrder = enhancedOrderService.createOrderWithDiscounts(
                    orderRequest.getUserId(),
                    orderRequest.getCartId(),
                    orderRequest.getBillingAddressId(),
                    orderRequest.getShippingAddressId(),
                    orderRequest.getCouponCodes()
            );
         log.info("Creating order with discounts for user: {}", orderRequest.getCouponCodes());
            // Add items if provided in the request
            if (orderRequest.getItems() != null && !orderRequest.getItems().isEmpty()) {
                List<OrderItem> items = orderMapper.toOrderItemList(orderRequest.getItems());
                for (OrderItem item : items) {
                    enhancedOrderService.addOrderItem(newOrder.getId(), item);
                }
                // Refresh the order to get updated items and total
                newOrder = enhancedOrderService.getOrderById(newOrder.getId());
            }

            OrderResponseDto responseDto = orderMapper.toOrderResponseDto(newOrder);
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating order with discounts: " + e.getMessage());
        }
    }

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
    private UUID parseUUID(String uuidString) {
        // Remove any existing hyphens
        String cleanUuid = uuidString.replaceAll("-", "");

        // Handle MongoDB ObjectId (24 characters) by padding to UUID format
        if (cleanUuid.length() == 24 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Pad with zeros to make it 32 characters
            cleanUuid = cleanUuid + "00000000";
        }

        // Check if it's exactly 32 hex characters
        if (cleanUuid.length() == 32 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Insert hyphens at correct positions: 8-4-4-4-12
            String formattedUuid = cleanUuid.substring(0, 8) + "-" +
                    cleanUuid.substring(8, 12) + "-" +
                    cleanUuid.substring(12, 16) + "-" +
                    cleanUuid.substring(16, 20) + "-" +
                    cleanUuid.substring(20, 32);
            return UUID.fromString(formattedUuid);
        }

        // Try parsing as-is (in case it's already properly formatted)
        return UUID.fromString(uuidString);
    }


//
@Autowired
private PaymentIntegrationService paymentIntegrationService;

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<Map<String, Object>> processOrderPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentMethodRequestDto paymentRequest) {
        try {
            log.info("💳 Processing payment for order: {}", orderId);

            // Get order to validate
            Order order = orderService.getOrderById(orderId);

            if (order.getStatus() != OrderStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Order must be in PENDING status to process payment");
            }

            // Process payment through Payment Service
            PaymentResponseDto paymentResponse = paymentIntegrationService.processOrderPayment(
                    orderId,
                    paymentRequest.getPaymentMethod(),
                    order.getTotalAmount()
            );

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("orderStatus", order.getStatus());
            response.put("payment", paymentResponse);
            response.put("message", "Payment processing initiated");

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("💳 Error processing payment for order {}: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payment processing failed: " + e.getMessage());
        }
    }

    /**
     * Get payment status for an order
     */
    @GetMapping("/{orderId}/payment/status")
    public ResponseEntity<Map<String, Object>> getOrderPaymentStatus(@PathVariable UUID orderId) {
        try {
            log.info("💳 Getting payment status for order: {}", orderId);

            // Verify order exists
            Order order = orderService.getOrderById(orderId);

            // Get payment status from Payment Service
            PaymentResponseDto paymentStatus = paymentIntegrationService.getOrderPaymentStatus(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("orderStatus", order.getStatus());
            response.put("orderTotal", order.getTotalAmount());
            response.put("paymentStatus", paymentStatus);

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("💳 Error getting payment status for order {}: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get payment status: " + e.getMessage());
        }
    }

    /**
     * Refund payment for an order
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<Map<String, Object>> refundOrderPayment(
            @PathVariable UUID orderId,
            @RequestBody Map<String, Object> refundRequest) {
        try {
            log.info("💳 Processing refund for order: {}", orderId);

            // Get order to validate
            Order order = orderService.getOrderById(orderId);

            if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Order must be PAID or COMPLETED to process refund");
            }

            // Extract refund details
            BigDecimal refundAmount = new BigDecimal(refundRequest.get("amount").toString());
            String reason = (String) refundRequest.getOrDefault("reason", "Customer request");

            // Process refund through Payment Service
            PaymentResponseDto refundResponse = paymentIntegrationService.refundOrderPayment(
                    orderId, refundAmount, reason
            );

            // Update order status if full refund
            if (refundAmount.compareTo(order.getTotalAmount()) == 0) {
                orderService.updateOrderStatus(orderId, OrderStatus.REFUNDED);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("refund", refundResponse);
            response.put("message", "Refund processed successfully");

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("💳 Error processing refund for order {}: {}", orderId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Refund processing failed: " + e.getMessage());
        }
    }

    /**
     * Create order and process payment in one step
     */
    @PostMapping("/with-payment")
    public ResponseEntity<Map<String, Object>> createOrderWithPayment(
            @Valid @RequestBody CreateOrderWithPaymentRequestDto orderRequest) {
        try {
            log.info("💳 Creating order with payment for user: {}", orderRequest.getUserId());

            // Create order first
            Order newOrder = orderService.createOrder(
                    orderRequest.getUserId(),
                    orderRequest.getCartId(),
                    orderRequest.getBillingAddressId(),
                    orderRequest.getShippingAddressId()
            );

            // Add items if provided
            if (orderRequest.getItems() != null && !orderRequest.getItems().isEmpty()) {
                List<OrderItem> items = orderMapper.toOrderItemList(orderRequest.getItems());
                for (OrderItem item : items) {
                    orderService.addOrderItem(newOrder.getId(), item);
                }
                // Refresh the order to get updated total
                newOrder = orderService.getOrderById(newOrder.getId());
            }

            // Process payment if payment method provided
            PaymentResponseDto paymentResponse = null;
            if (orderRequest.getPaymentMethod() != null && !orderRequest.getPaymentMethod().isEmpty()) {
                paymentResponse = paymentIntegrationService.processOrderPayment(
                        newOrder.getId(),
                        orderRequest.getPaymentMethod(),
                        newOrder.getTotalAmount()
                );
            }

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("order", orderMapper.toOrderResponseDto(newOrder));
            response.put("payment", paymentResponse);
            response.put("message", "Order created" + (paymentResponse != null ? " and payment initiated" : ""));

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("💳 Error creating order with payment: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Order creation with payment failed: " + e.getMessage());
        }
    }

    // Add this DTO class for the combined create order with payment request
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateOrderWithPaymentRequestDto {
        @NotNull(message = "User ID is required")
        private String userId;

        @NotNull(message = "Cart ID is required")
        private UUID cartId;

        @NotNull(message = "Billing address ID is required")
        private UUID billingAddressId;

        @NotNull(message = "Shipping address ID is required")
        private UUID shippingAddressId;

        private List<CreateOrderItemRequestDto> items;

        private String paymentMethod;
    }
}