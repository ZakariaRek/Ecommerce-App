package com.Ecommerce.Product_Service.Entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String contactInfo;
    private String address;

    @Column(columnDefinition = "json")
    private String contractDetails; // Stored as JSON string

    private BigDecimal rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "suppliers")
    private List<Product> products = new ArrayList<>();

    public void provideProduct() {
        // Implementation logic here
    }

    public void updateSupplierInfo() {
        // Implementation logic here
    }

    public void rateSupplier() {
        // Implementation logic here
    }
}
