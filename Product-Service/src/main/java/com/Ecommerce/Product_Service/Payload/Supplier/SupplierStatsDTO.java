package com.Ecommerce.Product_Service.Payload.Supplier;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierStatsDTO {
    private long totalSuppliers;
    private BigDecimal averageRating;
    private BigDecimal highestRating;
    private BigDecimal lowestRating;
    private long totalProducts;
    private String topRatedSupplierName;
    private LocalDateTime lastUpdated;

    public SupplierStatsDTO() {
        this.lastUpdated = LocalDateTime.now();
    }
}