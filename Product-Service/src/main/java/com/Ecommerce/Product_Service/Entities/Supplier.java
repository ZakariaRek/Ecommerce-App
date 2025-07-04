package com.Ecommerce.Product_Service.Entities;

import com.Ecommerce.Product_Service.Config.JsonConverter;
import com.Ecommerce.Product_Service.Listener.SupplierEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Data
@Table(name = "suppliers")
@EntityListeners(SupplierEntityListener.class)
public class Supplier {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String contactInfo;
    private String address;

    @Column(columnDefinition = "text")
    @Convert(converter = JsonConverter.class)
    private Map<String, Object> contractDetails; // Changed from String to Map<String, Object>

    private BigDecimal rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToMany(mappedBy = "suppliers")
    private List<Product> products = new ArrayList<>();

}
