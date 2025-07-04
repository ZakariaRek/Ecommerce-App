package com.Ecommerce.Product_Service.Entities;

import com.Ecommerce.Product_Service.Listener.CategoryEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "categories")
@EntityListeners(CategoryEntityListener.class)
public class Category {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    private String description;
    private String imageUrl;
    private Integer level;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @ManyToMany(mappedBy = "categories")
    private List<Product> products = new ArrayList<>();


}
