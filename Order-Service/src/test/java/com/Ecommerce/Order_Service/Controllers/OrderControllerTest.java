package com.Ecommerce.Order_Service.Controllers;


import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

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

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
        objectMapper = new ObjectMapper();

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
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setStatus(OrderStatus.PENDING);

        testOrderItem = new OrderItem();
        testOrderItem.setId(testItemId);
        testOrderItem.setProductId(UUID.randomUUID());
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(29.99));
    }

    @Test
    public void testCreateOrder_Success() throws Exception {
        // Prepare test data
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("userId", testUserId.toString());
        orderRequest.put("cartId", testCartId.toString());
        orderRequest.put("billingAddressId", testBillingAddressId.toString());
        orderRequest.put("shippingAddressId", testShippingAddressId.toString());

        when(orderService.createOrder(
                eq(testUserId),
                eq(testCartId),
                eq(testBillingAddressId),
                eq(testShippingAddressId)
        )).thenReturn(testOrder);

        // Perform and verify the request
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.status").value(testOrder.getStatus().toString()));

        verify(orderService).createOrder(testUserId, testCartId, testBillingAddressId, testShippingAddressId);
    }

    @Test
    public void testCreateOrder_InvalidUuid() throws Exception {
        // Prepare test data with invalid UUID
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("userId", "invalid-uuid");
        orderRequest.put("cartId", testCartId.toString());
        orderRequest.put("billingAddressId", testBillingAddressId.toString());
        orderRequest.put("shippingAddressId", testShippingAddressId.toString());

        // Perform and verify the request
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    public void testCreateOrder_ServerError() throws Exception {
        // Prepare test data
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("userId", testUserId.toString());
        orderRequest.put("cartId", testCartId.toString());
        orderRequest.put("billingAddressId", testBillingAddressId.toString());
        orderRequest.put("shippingAddressId", testShippingAddressId.toString());

        when(orderService.createOrder(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Perform and verify the request
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isInternalServerError());

        verify(orderService).createOrder(testUserId, testCartId, testBillingAddressId, testShippingAddressId);
    }

    @Test
    public void testGetOrderById_Success() throws Exception {
        when(orderService.getOrderById(testOrderId)).thenReturn(testOrder);

        mockMvc.perform(get("/api/orders/{orderId}", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.status").value(testOrder.getStatus().toString()));

        verify(orderService).getOrderById(testOrderId);
    }

    @Test
    public void testGetOrderById_NotFound() throws Exception {
        when(orderService.getOrderById(testOrderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/{orderId}", testOrderId))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(testOrderId);
    }

    @Test
    public void testGetOrdersByUser_Success() throws Exception {
        List<Order> orders = List.of(testOrder);
        when(orderService.getOrdersByUserId(testUserId)).thenReturn(orders);

        mockMvc.perform(get("/api/orders/user/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testOrderId.toString()))
                .andExpect(jsonPath("$[0].userId").value(testUserId.toString()))
                .andExpect(jsonPath("$[0].status").value(testOrder.getStatus().toString()));

        verify(orderService).getOrdersByUserId(testUserId);
    }

    @Test
    public void testGetOrdersByUser_EmptyList() throws Exception {
        when(orderService.getOrdersByUserId(testUserId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/orders/user/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(orderService).getOrdersByUserId(testUserId);
    }

    @Test
    public void testUpdateOrderStatus_Success() throws Exception {
        Map<String, String> statusUpdate = Map.of("status", "SHIPPED");
        Order updatedOrder = new Order();
        updatedOrder.setId(testOrderId);
        updatedOrder.setUserId(testUserId);
        updatedOrder.setStatus(OrderStatus.SHIPPED);

        when(orderService.updateOrderStatus(eq(testOrderId), eq(OrderStatus.SHIPPED)))
                .thenReturn(updatedOrder);

        mockMvc.perform(patch("/api/orders/{orderId}/status", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.SHIPPED);
    }

    @Test
    public void testUpdateOrderStatus_InvalidStatus() throws Exception {
        Map<String, String> statusUpdate = Map.of("status", "INVALID_STATUS");

        mockMvc.perform(patch("/api/orders/{orderId}/status", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderStatus(any(), any());
    }

    @Test
    public void testUpdateOrderStatus_OrderNotFound() throws Exception {
        Map<String, String> statusUpdate = Map.of("status", "SHIPPED");

        when(orderService.updateOrderStatus(eq(testOrderId), any()))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(patch("/api/orders/{orderId}/status", testOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isNotFound());

        verify(orderService).updateOrderStatus(eq(testOrderId), any());
    }


    @Test
    public void testCancelOrder_NotFound() throws Exception {
        when(orderService.cancelOrder(testOrderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", testOrderId))
                .andExpect(status().isNotFound());

        verify(orderService).cancelOrder(testOrderId);
    }

    @Test
    public void testCancelOrder_InvalidState() throws Exception {
        when(orderService.cancelOrder(testOrderId))
                .thenThrow(new IllegalStateException("Order cannot be cancelled in current state"));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", testOrderId))
                .andExpect(status().isBadRequest());

        verify(orderService).cancelOrder(testOrderId);
    }

    @Test
    public void testGenerateInvoice_Success() throws Exception {
        String invoiceContent = "Invoice content for order " + testOrderId;

        when(orderService.generateInvoice(testOrderId)).thenReturn(invoiceContent);

        mockMvc.perform(get("/api/orders/{orderId}/invoice", testOrderId))
                .andExpect(status().isOk())
                .andExpect(content().string(invoiceContent));

        verify(orderService).generateInvoice(testOrderId);
    }

    @Test
    public void testGenerateInvoice_OrderNotFound() throws Exception {
        when(orderService.generateInvoice(testOrderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/{orderId}/invoice", testOrderId))
                .andExpect(status().isNotFound());

        verify(orderService).generateInvoice(testOrderId);
    }


    @Test
    public void testGetOrderItems_OrderNotFound() throws Exception {
        when(orderService.getOrderItems(testOrderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/{orderId}/items", testOrderId))
                .andExpect(status().isNotFound());

        verify(orderService).getOrderItems(testOrderId);
    }







    @Test
    public void testUpdateItemQuantity_ItemNotFound() throws Exception {
        Map<String, Integer> quantityUpdate = Map.of("quantity", 5);

        when(orderService.updateOrderItemQuantity(testOrderId, testItemId, 5))
                .thenThrow(new EntityNotFoundException("Order item not found"));

        mockMvc.perform(patch("/api/orders/{orderId}/items/{itemId}", testOrderId, testItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quantityUpdate)))
                .andExpect(status().isNotFound());

        verify(orderService).updateOrderItemQuantity(testOrderId, testItemId, 5);
    }

    @Test
    public void testUpdateItemQuantity_InvalidQuantity() throws Exception {
        Map<String, Integer> quantityUpdate = Map.of("quantity", -1);

        when(orderService.updateOrderItemQuantity(testOrderId, testItemId, -1))
                .thenThrow(new IllegalArgumentException("Quantity must be positive"));

        mockMvc.perform(patch("/api/orders/{orderId}/items/{itemId}", testOrderId, testItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quantityUpdate)))
                .andExpect(status().isBadRequest());

        verify(orderService).updateOrderItemQuantity(testOrderId, testItemId, -1);
    }

    @Test
    public void testCalculateOrderTotal_Success() throws Exception {
        BigDecimal total = BigDecimal.valueOf(59.98);

        when(orderService.calculateOrderTotal(testOrderId)).thenReturn(total);

        mockMvc.perform(get("/api/orders/{orderId}/total", testOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(59.98));

        verify(orderService).calculateOrderTotal(testOrderId);
    }

    @Test
    public void testCalculateOrderTotal_OrderNotFound() throws Exception {
        when(orderService.calculateOrderTotal(testOrderId))
                .thenThrow(new EntityNotFoundException("Order not found"));

        mockMvc.perform(get("/api/orders/{orderId}/total", testOrderId))
                .andExpect(status().isNotFound());

        verify(orderService).calculateOrderTotal(testOrderId);
    }
}