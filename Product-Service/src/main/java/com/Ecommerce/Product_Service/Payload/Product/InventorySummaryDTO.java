package com.Ecommerce.Product_Service.Payload.Product;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InventorySummaryDTO {
    private UUID id;
    private UUID productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private Boolean available;
    private String warehouseLocation;
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private LocalDateTime lastRestocked;
}