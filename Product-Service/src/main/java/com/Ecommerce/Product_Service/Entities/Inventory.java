package com.Ecommerce.Product_Service.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "inventory")
public class Inventory {
    @Id
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;
    private Integer lowStockThreshold;
    private LocalDateTime lastRestocked;
    private String warehouseLocation;

    public void updateStock() {
        // Implementation logic here
    }

    public boolean checkAvailability() {
        // Implementation logic here
        return quantity > 0;
    }

    public void notifyLowStock() {
        // Implementation logic here
    }
}