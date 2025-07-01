package com.Ecommerce.Product_Service.Payload.Inventory;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
public class InventoryResponseDTO {
    private UUID id;
    private UUID productId;
    private int quantity;
    private boolean available;
    private String warehouseLocation;
    private int reserved;
    private LocalDateTime lastUpdated;

    // Getters and setters

}