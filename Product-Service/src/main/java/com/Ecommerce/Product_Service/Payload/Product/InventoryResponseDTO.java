package com.Ecommerce.Product_Service.Payload.Product;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Basic InventoryResponseDTO for simple use cases
 * Use this for basic inventory operations and listing views
 */
@Data
public class InventoryResponseDTO {

    // Core inventory fields
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private Boolean available;
    private String warehouseLocation;
    private Integer reserved;
    private LocalDateTime lastUpdated;

    // Essential product information
    private String productName;
    private String productSku;

    // Essential computed fields
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private Boolean isOutOfStock;

    // Constructor
    public InventoryResponseDTO() {
        this.reserved = 0; // Default value
    }

    // Helper methods
    public Boolean getIsLowStock() {
        if (quantity == null || lowStockThreshold == null) {
            return false;
        }
        return quantity <= lowStockThreshold;
    }

    public Boolean getIsOutOfStock() {
        return quantity != null && quantity == 0;
    }

    public Integer getAvailableQuantity() {
        if (quantity == null) return 0;
        if (reserved == null) return quantity;
        return Math.max(0, quantity - reserved);
    }

    public String getStockStatus() {
        if (getIsOutOfStock()) {
            return "OUT_OF_STOCK";
        } else if (getIsLowStock()) {
            return "LOW_STOCK";
        } else {
            return "IN_STOCK";
        }
    }
}