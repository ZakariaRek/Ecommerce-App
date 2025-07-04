package com.Ecommerce.Product_Service.Entities;

import com.Ecommerce.Product_Service.Listener.ProductEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "products")
@EntityListeners(ProductEntityListener.class)  // Added EntityListener for Kafka events
public class Product {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String sku;
    private BigDecimal weight;

    @Column(columnDefinition = "TEXT")
    private String dimensions;
//    @Column(columnDefinition = "json")
//    private String dimensions;


    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private Inventory inventory;

    @ManyToMany
    @JoinTable(
            name = "product_suppliers",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "supplier_id")
    )
    private List<Supplier> suppliers = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Review> reviews = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<Discount> discounts = new ArrayList<>();

    // Transient fields to track changes for Kafka events
    @Transient
    private BigDecimal previousPrice;

    @Transient
    private Integer previousStock;

    @Transient
    private ProductStatus previousStatus;

    // Initialize tracking fields before update
    @PreUpdate
    private void preUpdate() {
        // These will be used by the EntityListener to determine if events should be published
        this.previousPrice = this.price;
        this.previousStock = this.stock;
        this.previousStatus = this.status;
    }

    public void addToCart() {
        // Implementation logic here
    }

    public void updateProduct() {
        // Implementation logic here
    }

    public void changeStatus(ProductStatus newStatus) {
        this.previousStatus = this.status;
        this.status = newStatus;
        // Status change will trigger a Kafka event via EntityListener
    }

    public void updateStock(Integer newStock) {
        this.previousStock = this.stock;
        this.stock = newStock;
        // Stock change will trigger a Kafka event via EntityListener
    }

    public void updatePrice(BigDecimal newPrice) {
        this.previousPrice = this.price;
        this.price = newPrice;
        // Price change will trigger a Kafka event via EntityListener
    }
}