package com.Ecommerce.Product_Service.Entities;


import com.Ecommerce.Product_Service.Listener.DiscountEntityListener;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "discounts")
@EntityListeners(DiscountEntityListener.class)
public class Discount {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal minPurchaseAmount;
    private BigDecimal maxDiscountAmount;

    public BigDecimal applyDiscount(BigDecimal price) {
        // Implementation logic here
        return price;
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return (now.isAfter(startDate) || now.isEqual(startDate)) &&
                (now.isBefore(endDate) || now.isEqual(endDate));
    }
}

