package com.Ecommerce.Order_Service.Entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderEntityTest {

    private UUID testOrderId;
    private UUID testUserId;
    private UUID testCartId;
    private UUID testBillingAddressId;
    private UUID testShippingAddressId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testCartId = UUID.randomUUID();
        testBillingAddressId = UUID.randomUUID();
        testShippingAddressId = UUID.randomUUID();

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
    }

    @Test
    void createOrder_WithValidParameters_CreatesOrderWithCorrectDefaults() {
        // When
        Order order = Order.createOrder(testUserId, testCartId, testBillingAddressId, testShippingAddressId);

        // Then
        assertThat(order).isNotNull();
        assertThat(order.getUserId()).isEqualTo(testUserId);
        assertThat(order.getCartId()).isEqualTo(testCartId);
        assertThat(order.getBillingAddressId()).isEqualTo(testBillingAddressId);
        assertThat(order.getShippingAddressId()).isEqualTo(testShippingAddressId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getTax()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getShippingCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify new discount fields are initialized
        assertThat(order.getProductDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getOrderLevelDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getLoyaltyCouponDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.getTierBenefitDiscount()).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(order.getId()).isNull(); // Should be null until persisted
    }

    @Test
    void updateStatus_WithValidStatus_UpdatesSuccessfully() {
        // Given
        OrderStatus newStatus = OrderStatus.SHIPPED;

        // When
        testOrder.updateStatus(newStatus);

        // Then
        assertThat(testOrder.getStatus()).isEqualTo(newStatus);
    }

    @Test
    void cancelOrder_SetsStatusToCanceled() {
        // When
        testOrder.cancelOrder();

        // Then
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void generateInvoice_ReturnsCorrectInvoiceString() {
        // When
        String invoice = testOrder.generateInvoice();

        // Then
        assertThat(invoice).isEqualTo("Invoice for Order " + testOrderId);
    }

    @Test
    void getTotalDiscount_WithAllDiscountTypes_CalculatesCorrectTotal() {
        // Given
        testOrder.setProductDiscount(BigDecimal.valueOf(10.00));
        testOrder.setOrderLevelDiscount(BigDecimal.valueOf(5.00));
        testOrder.setLoyaltyCouponDiscount(BigDecimal.valueOf(15.00));
        testOrder.setTierBenefitDiscount(BigDecimal.valueOf(8.00));

        // When
        BigDecimal totalDiscount = testOrder.getTotalDiscount();

        // Then
        assertThat(totalDiscount).isEqualByComparingTo(BigDecimal.valueOf(38.00));
    }

    @Test
    void getTotalDiscount_WithZeroDiscounts_ReturnsZero() {
        // Given
        testOrder.setProductDiscount(BigDecimal.ZERO);
        testOrder.setOrderLevelDiscount(BigDecimal.ZERO);
        testOrder.setLoyaltyCouponDiscount(BigDecimal.ZERO);
        testOrder.setTierBenefitDiscount(BigDecimal.ZERO);

        // When
        BigDecimal totalDiscount = testOrder.getTotalDiscount();

        // Then
        assertThat(totalDiscount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalDiscount_WithNullDiscounts_HandleGracefully() {
        // Given - When discount fields are null (default state)
        testOrder.setProductDiscount(null);
        testOrder.setOrderLevelDiscount(null);
        testOrder.setLoyaltyCouponDiscount(null);
        testOrder.setTierBenefitDiscount(null);

        // When & Then - Should handle null values gracefully
        assertThatCode(() -> testOrder.getTotalDiscount())
                .doesNotThrowAnyException();
    }

    @Test
    void prePersist_SetsCreatedAtAndUpdatedAt() {
        // Given
        Order newOrder = new Order();
        LocalDateTime beforePersist = LocalDateTime.now().minusSeconds(1);

        // When
        newOrder.onCreate(); // Simulate @PrePersist

        // Then
        assertThat(newOrder.getCreatedAt()).isNotNull();
        assertThat(newOrder.getUpdatedAt()).isNotNull();
        assertThat(newOrder.getCreatedAt()).isAfter(beforePersist);
        assertThat(newOrder.getUpdatedAt()).isAfter(beforePersist);
        assertThat(newOrder.getCreatedAt()).isEqualTo(newOrder.getUpdatedAt());
    }

    @Test
    void preUpdate_UpdatesOnlyUpdatedAt() {
        // Given
        LocalDateTime originalCreatedAt = LocalDateTime.now().minusHours(1);
        testOrder.setCreatedAt(originalCreatedAt);
        testOrder.setUpdatedAt(originalCreatedAt);

        // When
        testOrder.onUpdate(); // Simulate @PreUpdate

        // Then
        assertThat(testOrder.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(testOrder.getUpdatedAt()).isAfter(originalCreatedAt);
    }

    @Test
    void setItems_WithValidList_SetsCorrectly() {
        // Given
        List<OrderItem> items = new ArrayList<>();
        OrderItem item1 = new OrderItem();
        item1.setId(UUID.randomUUID());
        items.add(item1);

        // When
        testOrder.setItems(items);

        // Then
        assertThat(testOrder.getItems()).hasSize(1);
        assertThat(testOrder.getItems().get(0)).isEqualTo(item1);
    }

    @Test
    void setItems_WithNullList_HandlesGracefully() {
        // When
        testOrder.setItems(null);

        // Then
        assertThat(testOrder.getItems()).isNull();
    }

    @Test
    void setDiscountApplications_WithValidList_SetsCorrectly() {
        // Given
        List<DiscountApplication> applications = new ArrayList<>();
        DiscountApplication app1 = new DiscountApplication();
        app1.setId(UUID.randomUUID());
        applications.add(app1);

        // When
        testOrder.setDiscountApplications(applications);

        // Then
        assertThat(testOrder.getDiscountApplications()).hasSize(1);
        assertThat(testOrder.getDiscountApplications().get(0)).isEqualTo(app1);
    }

    @Test
    void toString_DoesNotCauseStackOverflow() {
        // Given
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(testOrder);
        testOrder.setItems(List.of(item));

        // When & Then - Should not cause stack overflow due to circular reference
        assertThatCode(() -> {
            String result = testOrder.toString();
            assertThat(result).contains(testOrderId.toString());
            assertThat(result).contains("itemCount=1");
        }).doesNotThrowAnyException();
    }

    @Test
    void equals_WithSameId_ReturnsTrue() {
        // Given
        Order order1 = new Order();
        order1.setId(testOrderId);

        Order order2 = new Order();
        order2.setId(testOrderId);

        // When & Then
        assertThat(order1).isEqualTo(order2);
    }

    @Test
    void equals_WithDifferentIds_ReturnsFalse() {
        // Given
        Order order1 = new Order();
        order1.setId(testOrderId);

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());

        // When & Then
        assertThat(order1).isNotEqualTo(order2);
    }

    @Test
    void hashCode_WithSameId_ReturnsSameHashCode() {
        // Given
        Order order1 = new Order();
        order1.setId(testOrderId);

        Order order2 = new Order();
        order2.setId(testOrderId);

        // When & Then
        assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
    }

    @Test
    void discountFieldsDefaults_AreSetCorrectly() {
        // Given
        Order newOrder = new Order();

        // When - Check default values
        assertThat(newOrder.getProductDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(newOrder.getOrderLevelDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(newOrder.getLoyaltyCouponDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(newOrder.getTierBenefitDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void setDiscountFields_WithValidValues_SetsCorrectly() {
        // When
        testOrder.setProductDiscount(BigDecimal.valueOf(12.50));
        testOrder.setOrderLevelDiscount(BigDecimal.valueOf(8.75));
        testOrder.setLoyaltyCouponDiscount(BigDecimal.valueOf(20.00));
        testOrder.setTierBenefitDiscount(BigDecimal.valueOf(5.25));

        // Then
        assertThat(testOrder.getProductDiscount()).isEqualByComparingTo(BigDecimal.valueOf(12.50));
        assertThat(testOrder.getOrderLevelDiscount()).isEqualByComparingTo(BigDecimal.valueOf(8.75));
        assertThat(testOrder.getLoyaltyCouponDiscount()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
        assertThat(testOrder.getTierBenefitDiscount()).isEqualByComparingTo(BigDecimal.valueOf(5.25));
    }

    @Test
    void setDiscountBreakdown_WithValidJson_SetsCorrectly() {
        // Given
        String discountBreakdown = "{\"type\":\"PRODUCT\",\"amount\":10.00}";

        // When
        testOrder.setDiscountBreakdown(discountBreakdown);

        // Then
        assertThat(testOrder.getDiscountBreakdown()).isEqualTo(discountBreakdown);
    }

    @Test
    void setAppliedCouponCodes_WithValidJson_SetsCorrectly() {
        // Given
        String couponCodes = "[\"SAVE10\", \"SUMMER20\"]";

        // When
        testOrder.setAppliedCouponCodes(couponCodes);

        // Then
        assertThat(testOrder.getAppliedCouponCodes()).isEqualTo(couponCodes);
    }

    @Test
    void setDiscountRulesVersion_WithValidVersion_SetsCorrectly() {
        // Given
        String version = "v1.2.3";

        // When
        testOrder.setDiscountRulesVersion(version);

        // Then
        assertThat(testOrder.getDiscountRulesVersion()).isEqualTo(version);
    }
}