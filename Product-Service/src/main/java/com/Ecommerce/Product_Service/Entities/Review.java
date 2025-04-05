package com.Ecommerce.Product_Service.Entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer rating;
    private String comment;
    private Boolean verified;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addReview() {
        // Implementation logic here
    }

    public void updateReview() {
        // Implementation logic here
    }

    public void verifyReview() {
        // Implementation logic here
    }
}
