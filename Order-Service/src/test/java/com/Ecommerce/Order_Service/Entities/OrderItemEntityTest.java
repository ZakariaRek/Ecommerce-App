package com.Ecommerce.Order_Service.Entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderItemEntityTest {

    private UUID testOrderItemId;
    private UUID testOrderId;
    private UUID testProductId;
    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrderItemId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testProductId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(testOrderId);

        testOrderItem = new OrderItem();
        testOrderItem.setId(testOrderItemId);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProductId(testProductId);
        testOrderItem.setQuantity(2);
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(50.00));
        testOrderItem.setDiscount(BigDecimal.valueOf(10.00));
    }

    @Test
    void getTotal_WithValidPriceAndQuantity_CalculatesCorrectly() {
        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (50.00 * 2) - 10.00 = 90.00
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(90.00));
    }

    @Test
    void getTotal_WithZeroDiscount_CalculatesCorrectly() {
        // Given
        testOrderItem.setDiscount(BigDecimal.ZERO);

        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (50.00 * 2) - 0.00 = 100.00
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void getTotal_WithLargeDiscount_CalculatesCorrectly() {
        // Given
        testOrderItem.setDiscount(BigDecimal.valueOf(75.00));

        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (50.00 * 2) - 75.00 = 25.00
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(25.00));
    }

    @Test
    void getTotal_WithDiscountEqualToSubtotal_ReturnsZero() {
        // Given
        testOrderItem.setDiscount(BigDecimal.valueOf(100.00)); // Equal to subtotal

        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (50.00 * 2) - 100.00 = 0.00
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotal_WithDiscountGreaterThanSubtotal_ReturnsNegative() {
        // Given
        testOrderItem.setDiscount(BigDecimal.valueOf(150.00)); // Greater than subtotal

        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (50.00 * 2) - 150.00 = -50.00
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(-50.00));
    }

    @Test
    void getTotal_WithSingleQuantity_CalculatesCorrectly() {
        // Given
        testOrderItem.setQuantity(1);

        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (50.00 * 1) - 10.00 = 40.00
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(40.00));
    }

    @Test
    void getTotal_WithDecimalPrice_CalculatesCorrectly() {
        // Given
        testOrderItem.setPriceAtPurchase(BigDecimal.valueOf(29.99));
        testOrderItem.setQuantity(3);
        testOrderItem.setDiscount(BigDecimal.valueOf(5.50));

        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (29.99 * 3) - 5.50 = 89.97 - 5.50 = 84.47
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(84.47));
    }

    @Test
    void updateQuantity_WithValidQuantity_UpdatesSuccessfully() {
        // Given
        int newQuantity = 5;

        // When
        testOrderItem.updateQuantity(newQuantity);

        // Then
        assertThat(testOrderItem.getQuantity()).isEqualTo(newQuantity);
    }

    @Test
    void updateQuantity_WithZeroQuantity_ThrowsIllegalArgumentException() {
        // When & Then
        assertThatThrownBy(() -> testOrderItem.updateQuantity(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    void updateQuantity_WithNegativeQuantity_ThrowsIllegalArgumentException() {
        // When & Then
        assertThatThrownBy(() -> testOrderItem.updateQuantity(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    void updateQuantity_WithLargeQuantity_UpdatesSuccessfully() {
        // Given
        int largeQuantity = 1000;

        // When
        testOrderItem.updateQuantity(largeQuantity);

        // Then
        assertThat(testOrderItem.getQuantity()).isEqualTo(largeQuantity);
    }

    @Test
    void toString_DoesNotCauseStackOverflow() {
        // When & Then - Should not cause stack overflow due to circular reference
        assertThatCode(() -> {
            String result = testOrderItem.toString();
            assertThat(result).contains(testOrderItemId.toString());
            assertThat(result).contains(testProductId.toString());
            assertThat(result).contains("quantity=2");
            assertThat(result).contains("orderId=" + testOrderId);
        }).doesNotThrowAnyException();
    }

    @Test
    void toString_WithNullOrder_HandlesGracefully() {
        // Given
        testOrderItem.setOrder(null);

        // When & Then
        assertThatCode(() -> {
            String result = testOrderItem.toString();
            assertThat(result).contains("orderId=null");
        }).doesNotThrowAnyException();
    }

    @Test
    void equals_WithSameId_ReturnsTrue() {
        // Given
        OrderItem item1 = new OrderItem();
        item1.setId(testOrderItemId);

        OrderItem item2 = new OrderItem();
        item2.setId(testOrderItemId);

        // When & Then
        assertThat(item1).isEqualTo(item2);
    }

    @Test
    void equals_WithDifferentIds_ReturnsFalse() {
        // Given
        OrderItem item1 = new OrderItem();
        item1.setId(testOrderItemId);

        OrderItem item2 = new OrderItem();
        item2.setId(UUID.randomUUID());

        // When & Then
        assertThat(item1).isNotEqualTo(item2);
    }

    @Test
    void hashCode_WithSameId_ReturnsSameHashCode() {
        // Given
        OrderItem item1 = new OrderItem();
        item1.setId(testOrderItemId);

        OrderItem item2 = new OrderItem();
        item2.setId(testOrderItemId);

        // When & Then
        assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
    }

    @Test
    void setOrder_WithValidOrder_SetsCorrectly() {
        // Given
        Order newOrder = new Order();
        newOrder.setId(UUID.randomUUID());

        // When
        testOrderItem.setOrder(newOrder);

        // Then
        assertThat(testOrderItem.getOrder()).isEqualTo(newOrder);
    }

    @Test
    void setOrder_WithNullOrder_SetsToNull() {
        // When
        testOrderItem.setOrder(null);

        // Then
        assertThat(testOrderItem.getOrder()).isNull();
    }

    @Test
    void setProductId_WithValidId_SetsCorrectly() {
        // Given
        UUID newProductId = UUID.randomUUID();

        // When
        testOrderItem.setProductId(newProductId);

        // Then
        assertThat(testOrderItem.getProductId()).isEqualTo(newProductId);
    }

    @Test
    void setPriceAtPurchase_WithValidPrice_SetsCorrectly() {
        // Given
        BigDecimal newPrice = BigDecimal.valueOf(75.50);

        // When
        testOrderItem.setPriceAtPurchase(newPrice);

        // Then
        assertThat(testOrderItem.getPriceAtPurchase()).isEqualByComparingTo(newPrice);
    }

    @Test
    void setPriceAtPurchase_WithZeroPrice_SetsCorrectly() {
        // Given
        BigDecimal zeroPrice = BigDecimal.ZERO;

        // When
        testOrderItem.setPriceAtPurchase(zeroPrice);

        // Then
        assertThat(testOrderItem.getPriceAtPurchase()).isEqualByComparingTo(zeroPrice);
    }

    @Test
    void setDiscount_WithValidDiscount_SetsCorrectly() {
        // Given
        BigDecimal newDiscount = BigDecimal.valueOf(25.00);

        // When
        testOrderItem.setDiscount(newDiscount);

        // Then
        assertThat(testOrderItem.getDiscount()).isEqualByComparingTo(newDiscount);
    }

    @Test
    void setDiscount_WithZeroDiscount_SetsCorrectly() {
        // Given
        BigDecimal zeroDiscount = BigDecimal.ZERO;

        // When
        testOrderItem.setDiscount(zeroDiscount);

        // Then
        assertThat(testOrderItem.getDiscount()).isEqualByComparingTo(zeroDiscount);
    }

    @Test
    void constructorAndGetters_WithAllFields_WorkCorrectly() {
        // Given
        OrderItem item = new OrderItem();
        UUID itemId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 3;
        BigDecimal price = BigDecimal.valueOf(100.00);
        BigDecimal discount = BigDecimal.valueOf(15.00);

        // When
        item.setId(itemId);
        item.setOrder(testOrder);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPriceAtPurchase(price);
        item.setDiscount(discount);

        // Then
        assertThat(item.getId()).isEqualTo(itemId);
        assertThat(item.getOrder()).isEqualTo(testOrder);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getQuantity()).isEqualTo(quantity);
        assertThat(item.getPriceAtPurchase()).isEqualByComparingTo(price);
        assertThat(item.getDiscount()).isEqualByComparingTo(discount);
    }

    @Test
    void getTotal_WithLargePrecisionNumbers_HandlesCorrectly() {
        // Given
        testOrderItem.setPriceAtPurchase(new BigDecimal("33.333333"));
        testOrderItem.setQuantity(3);
        testOrderItem.setDiscount(new BigDecimal("5.555555"));

        // When
        BigDecimal total = testOrderItem.getTotal();

        // Then
        // Total should be: (33.333333 * 3) - 5.555555 = 99.999999 - 5.555555 = 94.444444
        assertThat(total).isEqualByComparingTo(new BigDecimal("94.444444"));
    }
}