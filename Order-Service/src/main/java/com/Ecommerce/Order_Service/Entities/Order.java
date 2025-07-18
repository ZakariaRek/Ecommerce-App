package com.Ecommerce.Order_Service.Entities;

import com.Ecommerce.Order_Service.Listeners.OrderEntityListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(OrderEntityListener.class)
public class Order {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    // NEW DISCOUNT TRACKING FIELDS
    @Column(precision = 10, scale = 2)
    private BigDecimal productDiscount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal orderLevelDiscount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal loyaltyCouponDiscount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal tierBenefitDiscount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String discountBreakdown; // JSON field for detailed breakdown

    @Column(columnDefinition = "TEXT")
    private String appliedCouponCodes; // JSON array of applied coupon codes

    @Column(length = 10)
    private String discountRulesVersion; // Track which discount rules were applied

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private UUID billingAddressId;

    @Column(nullable = false)
    private UUID shippingAddressId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // NEW: Relationship to discount applications
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DiscountApplication> discountApplications = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to calculate total discount
    public BigDecimal getTotalDiscount() {
        return productDiscount
                .add(orderLevelDiscount)
                .add(loyaltyCouponDiscount)
                .add(tierBenefitDiscount);
    }

    // Existing methods...
    public static Order createOrder(UUID userId, UUID cartId, UUID billingAddressId, UUID shippingAddressId) {
        Order order = new Order();
        order.setUserId(userId);
        order.setCartId(cartId);
        order.setBillingAddressId(billingAddressId);
        order.setShippingAddressId(shippingAddressId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTax(BigDecimal.ZERO);
        order.setShippingCost(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        // Initialize new discount fields
        order.setProductDiscount(BigDecimal.ZERO);
        order.setOrderLevelDiscount(BigDecimal.ZERO);
        order.setLoyaltyCouponDiscount(BigDecimal.ZERO);
        order.setTierBenefitDiscount(BigDecimal.ZERO);
        return order;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public void cancelOrder() {
        this.status = OrderStatus.CANCELED;
    }

    public String generateInvoice() {
        return "Invoice for Order " + this.id;
    }
}
