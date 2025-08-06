package com.Ecommerce.Order_Service.Payload;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderItem;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Payload.Request.OrderItem.CreateOrderItemRequestDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.InvoiceResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.Order.OrderTotalResponseDto;
import com.Ecommerce.Order_Service.Payload.Response.OrderItem.OrderItemResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderMapperTest {

    private OrderMapper orderMapper;

    private UUID testOrderId;
    private UUID testUserId;
    private UUID testCartId;
    private UUID testBillingAddressId;
    private UUID testShippingAddressId;
    private UUID testItemId;
    private UUID testProductId;
    private Order testOrder;
    private OrderItem testOrderItem;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        orderMapper = new OrderMapper();

        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCartId = UUID.randomUUID();
        testBillingAddressId = UUID.randomUUID();
        testShippingAddressId = UUID.randomUUID();
        testItemId = UUID.randomUUID();
        testProductId = UUID.randomUUID();
        testDateTime = LocalDateTime.now();

        // Create test order
        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setUserId(testUserId);
        testOrder.setCartId(testCartId);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(100.00));
        testOrder.setTax(BigDecimal.valueOf(10.00));
        testOrder.setShippingCost(BigDecimal.valueOf(5.00));
        testOrder.setDiscount(BigDecimal.valueOf(15.00));
        testOrder.setBillingAddressId(testBillingAddressId);
        testOrder.setShippingAddressId(testShippingAddressId);
        testOrder.setCreatedAt(testDateTime);
        testOrder.setUpdatedAt(testDateTime);

        // Create test order item
        testOrderItem = new OrderItem();
        testOrderItem.setId(testItemId);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProductId(testProductId);
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(50.00));
        testOrderItem.setDiscount(BigDecimal.valueOf(10.00));

        // Add items to order
        testOrder.setItems(List.of(testOrderItem));
    }

    @Test
    void toOrderResponseDto_WithCompleteOrder_MapsAllFieldsCorrectly() {
        // When
        OrderResponseDto result = orderMapper.toOrderResponseDto(testOrder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testOrderId);
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getCartId()).isEqualTo(testCartId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(result.getTax()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(result.getShippingCost()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(15.00));
        assertThat(result.getBillingAddressId()).isEqualTo(testBillingAddressId);
        assertThat(result.getShippingAddressId()).isEqualTo(testShippingAddressId);
        assertThat(result.getCreatedAt()).isEqualTo(testDateTime);
        assertThat(result.getUpdatedAt()).isEqualTo(testDateTime);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getId()).isEqualTo(testItemId);
    }

    @Test
    void toOrderResponseDto_WithNullItems_HandlesGracefully() {
        // Given
        testOrder.setItems(null);

        // When
        OrderResponseDto result = orderMapper.toOrderResponseDto(testOrder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testOrderId);
        assertThat(result.getItems()).isNull();
    }

    @Test
    void toOrderResponseDto_WithEmptyItems_MapsEmptyList() {
        // Given
        testOrder.setItems(new ArrayList<>());

        // When
        OrderResponseDto result = orderMapper.toOrderResponseDto(testOrder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void toOrderItemResponseDto_WithCompleteOrderItem_MapsAllFieldsCorrectly() {
        // When
        OrderItemResponseDto result = orderMapper.toOrderItemResponseDto(testOrderItem);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItemId);
        assertThat(result.getProductId()).isEqualTo(testProductId);
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getPriceAtPurchase()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(10.00));

        // Verify total calculation: (50.00 * 2) - 10.00 = 90.00
        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(90.00));
    }

    @Test
    void toOrderItem_WithCreateOrderItemRequestDto_MapsAllFieldsCorrectly() {
        // Given
        CreateOrderItemRequestDto requestDto = CreateOrderItemRequestDto.builder()
                .productId(testProductId)
                .quantity(3)
                .priceAtPurchase(BigDecimal.valueOf(25.99))
                .discount(BigDecimal.valueOf(5.00))
                .build();

        // When
        OrderItem result = orderMapper.toOrderItem(requestDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(testProductId);
        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getPriceAtPurchase()).isEqualByComparingTo(BigDecimal.valueOf(25.99));
        assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(result.getOrder()).isNull(); // Order should be set separately
        assertThat(result.getId()).isNull(); // ID should be generated on save
    }

    @Test
    void toOrderItem_WithZeroDiscount_HandlesCorrectly() {
        // Given
        CreateOrderItemRequestDto requestDto = CreateOrderItemRequestDto.builder()
                .productId(testProductId)
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(100.00))
                .discount(BigDecimal.ZERO)
                .build();

        // When
        OrderItem result = orderMapper.toOrderItem(requestDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void toOrderItemList_WithValidList_MapsAllItems() {
        // Given
        List<CreateOrderItemRequestDto> requestDtos = List.of(
                CreateOrderItemRequestDto.builder()
                        .productId(UUID.randomUUID())
                        .quantity(1)
                        .priceAtPurchase(BigDecimal.valueOf(20.00))
                        .discount(BigDecimal.ZERO)
                        .build(),
                CreateOrderItemRequestDto.builder()
                        .productId(UUID.randomUUID())
                        .quantity(2)
                        .priceAtPurchase(BigDecimal.valueOf(30.00))
                        .discount(BigDecimal.valueOf(5.00))
                        .build()
        );

        // When
        List<OrderItem> result = orderMapper.toOrderItemList(requestDtos);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getQuantity()).isEqualTo(1);
        assertThat(result.get(0).getPriceAtPurchase()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        assertThat(result.get(1).getQuantity()).isEqualTo(2);
        assertThat(result.get(1).getPriceAtPurchase()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
    }

    @Test
    void toOrderItemList_WithNullList_ReturnsNull() {
        // When
        List<OrderItem> result = orderMapper.toOrderItemList(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toOrderItemList_WithEmptyList_ReturnsEmptyList() {
        // Given
        List<CreateOrderItemRequestDto> emptyList = new ArrayList<>();

        // When
        List<OrderItem> result = orderMapper.toOrderItemList(emptyList);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void toOrderTotalResponseDto_WithValidData_CalculatesCorrectly() {
        // Given
        BigDecimal total = BigDecimal.valueOf(100.00);

        // When
        OrderTotalResponseDto result = orderMapper.toOrderTotalResponseDto(total, testOrder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(result.getTax()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(result.getShippingCost()).isEqualByComparingTo(BigDecimal.valueOf(5.00));
        assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.valueOf(15.00));

        // Verify subtotal calculation: total - tax - shipping + discount = 100 - 10 - 5 + 15 = 100
        assertThat(result.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void toInvoiceResponseDto_WithValidData_MapsCorrectly() {
        // Given
        String invoiceData = "Invoice data for order " + testOrderId;

        // When
        InvoiceResponseDto result = orderMapper.toInvoiceResponseDto(invoiceData, testOrder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(testOrderId);
        assertThat(result.getInvoiceData()).isEqualTo(invoiceData);
        assertThat(result.getDownloadUrl()).isEqualTo("/api/orders/" + testOrderId + "/invoice/download");
    }

    @Test
    void toOrderResponseDtoList_WithMultipleOrders_MapsAllOrders() {
        // Given
        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setUserId(UUID.randomUUID());
        order2.setStatus(OrderStatus.SHIPPED);
        order2.setTotalAmount(BigDecimal.valueOf(200.00));
        order2.setTax(BigDecimal.valueOf(20.00));
        order2.setShippingCost(BigDecimal.valueOf(10.00));
        order2.setDiscount(BigDecimal.valueOf(30.00));
        order2.setCreatedAt(testDateTime);
        order2.setUpdatedAt(testDateTime);

        List<Order> orders = List.of(testOrder, order2);

        // When
        List<OrderResponseDto> result = orderMapper.toOrderResponseDtoList(orders);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(testOrderId);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.get(1).getId()).isEqualTo(order2.getId());
        assertThat(result.get(1).getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void toOrderItemResponseDtoList_WithMultipleItems_MapsAllItems() {
        // Given
        OrderItem item2 = new OrderItem();
        item2.setId(UUID.randomUUID());
        item2.setProductId(UUID.randomUUID());
        item2.setQuantity(1);
        item2.setPriceAtPurchase(BigDecimal.valueOf(75.00));
        item2.setDiscount(BigDecimal.valueOf(0.00));

        List<OrderItem> items = List.of(testOrderItem, item2);

        // When
        List<OrderItemResponseDto> result = orderMapper.toOrderItemResponseDtoList(items);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(testItemId);
        assertThat(result.get(0).getQuantity()).isEqualTo(2);
        assertThat(result.get(1).getId()).isEqualTo(item2.getId());
        assertThat(result.get(1).getQuantity()).isEqualTo(1);

        // Verify totals
        assertThat(result.get(0).getTotal()).isEqualByComparingTo(BigDecimal.valueOf(90.00)); // (50*2) - 10
        assertThat(result.get(1).getTotal()).isEqualByComparingTo(BigDecimal.valueOf(75.00)); // (75*1) - 0
    }
}