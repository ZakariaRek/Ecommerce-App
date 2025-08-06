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
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.payment.PaymentResponseDto;
import com.Ecommerce.Order_Service.Services.EnhancedOrderService;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.Ecommerce.Order_Service.Services.PaymentIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private EnhancedOrderService enhancedOrderService;

    @Mock
    private PaymentIntegrationService paymentIntegrationService;

    @InjectMocks
    private OrderController orderController;

    private ObjectMapper objectMapper;

    private UUID testOrderId;
    private UUID testUserId;
    private UUID testCartId;
    private UUID testBillingAddressId;
    private UUID testShippingAddressId;
    private UUID testItemId;
    private Order testOrder;
    private OrderItem testOrderItem;
    private OrderResponseDto testOrderResponseDto;
    private OrderItemResponseDto testOrderItemResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Initialize test data
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCartId = UUID.randomUUID();
        testBillingAddressId = UUID.randomUUID();
        testShippingAddressId = UUID.randomUUID();
        testItemId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setUserId(testUserId);
        testOrder.setCartId(testCartId);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setTax(BigDecimal.valueOf(10.00));
        testOrder.setShippingCost(BigDecimal.valueOf(5.00));
        testOrder.setDiscount(BigDecimal.ZERO);
        testOrder.setBillingAddressId(testBillingAddressId);
        testOrder.setShippingAddressId(testShippingAddressId);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        testOrderItem = new OrderItem();
        testOrderItem.setId(testItemId);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProductId(UUID.randomUUID());
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(29.99));
        testOrderItem.setDiscount(BigDecimal.ZERO);

        testOrderResponseDto = new OrderResponseDto();
        testOrderResponseDto.setId(testOrderId);
        testOrderResponseDto.setUserId(testUserId);
        testOrderResponseDto.setStatus(OrderStatus.PENDING);
        testOrderResponseDto.setTotalAmount(BigDecimal.valueOf(100.00));

        testOrderItemResponseDto = OrderItemResponseDto.builder()
                .id(testItemId)
                .productId(testOrderItem.getProductId())
                .quantity(2)
                .priceAtPurchase(BigDecimal.valueOf(29.99))
                .discount(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(59.98))
                .build();
    }

    @Test
    void getAllOrders_Success() throws Exception {
        List<Order> orders = List.of(testOrder);
        List<OrderResponseDto> orderDtos = List.of(testOrderResponseDto);

        when(orderService.getAllOrders()).thenReturn(orders);
        when(orderMapper.toOrderResponseDtoList(orders)).thenReturn(orderDtos);

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testOrderId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(orderService).getAllOrders();
        verify(orderMapper).toOrderResponseDtoList(orders);
    }

    @Test
    void createOrder_Success() throws Exception {
        CreateOrderRequestDto requestDto = new CreateOrderRequestDto();
        requestDto.setUserId(testUserId.toString());
        requestDto.setCartId(testCartId);
        requestDto.setBillingAddressId(testBillingAddressId);
        requestDto.setShippingAddressId(testShippingAddressId);

        when(orderService.createOrder(
                eq(testUserId.toString()),
                eq(testCartId),
                eq(testBillingAddressId),
                eq(testShippingAddressId)
        )).thenReturn(testOrder);
        when(orderMapper.toOrderResponseDto(testOrder)).thenReturn(testOrderResponseDto);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).createOrder(testUserId.toString(), testCartId, testBillingAddressId, testShippingAddressId);
        verify(orderMapper).toOrderResponseDto(testOrder);
    }

    @Test
    void createOrderWithDiscounts_Success() throws Exception {
        CreateOrderWithDiscountsRequestDto requestDto = new CreateOrderWithDiscountsRequestDto();
        requestDto.setUserId(testUserId.toString());
        requestDto.setCartId(testCartId);
        requestDto.setBillingAddressId(testBillingAddressId);
        requestDto.setShippingAddressId(testShippingAddressId);
        requestDto.setCouponCodes(List.of("SAVE10", "SUMMER"));

        when(enhancedOrderService.createOrderWithDiscounts(
                eq(testUserId.toString()),
                eq(testCartId),
                eq(testBillingAddressId),
                eq(testShippingAddressId),
                eq(List.of("SAVE10", "SUMMER"))
        )).thenReturn(testOrder);
        when(orderMapper.toOrderResponseDto(testOrder)).thenReturn(testOrderResponseDto);

        mockMvc.perform(post("/order/with-discounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()));

        verify(enhancedOrderService).createOrderWithDiscounts(
                testUserId.toString(), testCartId, testBillingAddressId, testShippingAddressId,
                List.of("SAVE10", "SUMMER")
        );
    }

    @Test
    void getOrderById_Success() throws Exception {
        when(orderService.getOrderById(testOrderId)).thenReturn(testOrder);
        when(orderMapper.toOrderResponseDto(testOrder)).thenReturn(testOrderResponseDto);

        mockMvc.perform(get("/order/{orderId}", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()));

        verify(orderService).getOrderById(testOrderId);
        verify(orderMapper).toOrderResponseDto(testOrder);
    }

    @Test
    void getOrderById_NotFound() throws Exception {
        when(orderService.getOrderById(testOrderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(get("/order/{orderId}", testOrderId))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(testOrderId);
    }

    @Test
    void getOrdersByUser_Success() throws Exception {
        List<Order> orders = List.of(testOrder);
        List<OrderResponseDto> orderDtos = List.of(testOrderResponseDto);

        when(orderService.getOrdersByUserId(testUserId)).thenReturn(orders);
        when(orderMapper.toOrderResponseDtoList(orders)).thenReturn(orderDtos);

        mockMvc.perform(get("/order/user/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testOrderId.toString()));

        verify(orderService).getOrdersByUserId(testUserId);
        verify(orderMapper).toOrderResponseDtoList(orders);
    }

    @Test
    void updateOrderStatus_Success() throws Exception {
        UpdateOrderStatusRequestDto requestDto = new UpdateOrderStatusRequestDto();
        requestDto.setStatus(OrderStatus.SHIPPED);

        Order updatedOrder = new Order();
        updatedOrder.setId(testOrderId);
        updatedOrder.setStatus(OrderStatus.SHIPPED);

        OrderResponseDto updatedDto = new OrderResponseDto();
        updatedDto.setId(testOrderId);
        updatedDto.setStatus(OrderStatus.SHIPPED);

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.SHIPPED)).thenReturn(updatedOrder);
        when(orderMapper.toOrderResponseDto(updatedOrder)).thenReturn(updatedDto);

        mockMvc.perform(patch("/order/{orderId}/status", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.SHIPPED);
    }

    @Test
    void cancelOrder_Success() throws Exception {
        Order canceledOrder = new Order();
        canceledOrder.setId(testOrderId);
        canceledOrder.setStatus(OrderStatus.CANCELED);

        OrderResponseDto canceledDto = new OrderResponseDto();
        canceledDto.setId(testOrderId);
        canceledDto.setStatus(OrderStatus.CANCELED);

        when(orderService.cancelOrder(testOrderId)).thenReturn(canceledOrder);
        when(orderMapper.toOrderResponseDto(canceledOrder)).thenReturn(canceledDto);

        mockMvc.perform(post("/order/{orderId}/cancel", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(orderService).cancelOrder(testOrderId);
    }

    @Test
    void cancelOrder_InvalidState() throws Exception {
        when(orderService.cancelOrder(testOrderId))
                .thenThrow(new IllegalStateException("Cannot cancel shipped order"));

        mockMvc.perform(post("/order/{orderId}/cancel", testOrderId))
                .andExpect(status().isBadRequest());

        verify(orderService).cancelOrder(testOrderId);
    }

    @Test
    void addOrderItem_Success() throws Exception {
        CreateOrderItemRequestDto requestDto = CreateOrderItemRequestDto.builder()
                .productId(UUID.randomUUID())
                .quantity(2)
                .priceAtPurchase(BigDecimal.valueOf(29.99))
                .discount(BigDecimal.ZERO)
                .build();

        when(orderMapper.toOrderItem(requestDto)).thenReturn(testOrderItem);
        when(orderService.addOrderItem(testOrderId, testOrderItem)).thenReturn(testOrderItem);
        when(orderMapper.toOrderItemResponseDto(testOrderItem)).thenReturn(testOrderItemResponseDto);

        mockMvc.perform(post("/order/{orderId}/items", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testItemId.toString()))
                .andExpect(jsonPath("$.quantity").value(2));

        verify(orderService).addOrderItem(testOrderId, testOrderItem);
    }

    @Test
    void updateItemQuantity_Success() throws Exception {
        UpdateOrderItemQuantityRequestDto requestDto = new UpdateOrderItemQuantityRequestDto();
        requestDto.setQuantity(5);

        OrderItem updatedItem = new OrderItem();
        updatedItem.setId(testItemId);
        updatedItem.setQuantity(5);

        OrderItemResponseDto updatedDto = OrderItemResponseDto.builder()
                .id(testItemId)
                .quantity(5)
                .build();

        when(orderService.updateOrderItemQuantity(testOrderId, testItemId, 5)).thenReturn(updatedItem);
        when(orderMapper.toOrderItemResponseDto(updatedItem)).thenReturn(updatedDto);

        mockMvc.perform(patch("/order/{orderId}/items/{itemId}", testOrderId, testItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testItemId.toString()))
                .andExpect(jsonPath("$.quantity").value(5));

        verify(orderService).updateOrderItemQuantity(testOrderId, testItemId, 5);
    }

    @Test
    void removeOrderItem_Success() throws Exception {
        doNothing().when(orderService).removeOrderItem(testOrderId, testItemId);

        mockMvc.perform(delete("/order/{orderId}/items/{itemId}", testOrderId, testItemId))
                .andExpect(status().isNoContent());

        verify(orderService).removeOrderItem(testOrderId, testItemId);
    }

    @Test
    void processOrderPayment_Success() throws Exception {
        PaymentMethodRequestDto requestDto = PaymentMethodRequestDto.builder()
                .paymentMethod("CREDIT_CARD")
                .currency("USD")
                .build();

        testOrder.setStatus(OrderStatus.PENDING);

        PaymentResponseDto paymentResponse = PaymentResponseDto.builder()
                .paymentId("pay_123")
                .orderId(testOrderId.toString())
                .status("COMPLETED")
                .success(true)
                .amount(BigDecimal.valueOf(100.00))
                .message("Payment successful")
                .build();

        when(orderService.getOrderById(testOrderId)).thenReturn(testOrder);
        when(paymentIntegrationService.processOrderPayment(testOrderId, "CREDIT_CARD", testOrder.getTotalAmount()))
                .thenReturn(paymentResponse);

        mockMvc.perform(post("/order/{orderId}/pay", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.payment.paymentId").value("pay_123"));

        verify(orderService).getOrderById(testOrderId);
        verify(paymentIntegrationService).processOrderPayment(testOrderId, "CREDIT_CARD", testOrder.getTotalAmount());
    }

    @Test
    void processOrderPayment_InvalidStatus() throws Exception {
        PaymentMethodRequestDto requestDto = PaymentMethodRequestDto.builder()
                .paymentMethod("CREDIT_CARD")
                .build();

        testOrder.setStatus(OrderStatus.SHIPPED);

        when(orderService.getOrderById(testOrderId)).thenReturn(testOrder);

        mockMvc.perform(post("/order/{orderId}/pay", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(orderService).getOrderById(testOrderId);
        verifyNoInteractions(paymentIntegrationService);
    }

    @Test
    void processOrderPayment_OrderNotFound() throws Exception {
        PaymentMethodRequestDto requestDto = PaymentMethodRequestDto.builder()
                .paymentMethod("CREDIT_CARD")
                .build();

        when(orderService.getOrderById(testOrderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(post("/order/{orderId}/pay", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(testOrderId);
        verifyNoInteractions(paymentIntegrationService);
    }
}