package com.Ecommerce.Order_Service.Entities;

import com.Ecommerce.Order_Service.Listeners.OrderItemEntityListener;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(OrderItemEntityListener.class)
public class OrderItem {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    /**
     * Calculates the total price for this order item
     * @return The total price after applying discount
     */
    public BigDecimal getTotal() {
        BigDecimal totalBeforeDiscount = priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
        return totalBeforeDiscount.subtract(discount);
    }

    /**
     * Updates the quantity of this order item
     * @param newQuantity The new quantity
     */
    public void updateQuantity(int newQuantity) {
        if (newQuantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = newQuantity;
    }
}