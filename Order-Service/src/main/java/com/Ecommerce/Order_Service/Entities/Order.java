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

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Creates a new order and initializes it with default values
     * @return The newly created order
     */
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
        return order;
    }

    /**
     * Updates the status of the order
     * @param status The new status for the order
     */
    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    /**
     * Cancels the order by setting its status to CANCELED
     */
    public void cancelOrder() {
        this.status = OrderStatus.CANCELED;
    }

    /**
     * Generates an invoice for this order
     * @return Invoice object (implementation not shown)
     */
    public String generateInvoice() {
        // This would typically generate and return an Invoice object or PDF
        return "Invoice for Order " + this.id;
    }
}