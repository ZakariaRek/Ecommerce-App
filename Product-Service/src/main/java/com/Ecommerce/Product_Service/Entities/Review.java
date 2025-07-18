package com.Ecommerce.Product_Service.Entities;

import com.Ecommerce.Product_Service.Listener.ReviewEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "reviews")
@EntityListeners(ReviewEntityListener.class)
public class Review {
    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;
    @JsonIgnore
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


}
