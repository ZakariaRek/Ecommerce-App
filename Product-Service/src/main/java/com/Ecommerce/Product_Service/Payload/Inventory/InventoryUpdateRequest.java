package com.Ecommerce.Product_Service.Payload.Inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;
@Data
public class InventoryUpdateRequest {

    private UUID id;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;



    @Min(value = 0, message = "Reserved quantity cannot be negative")
    private Integer reserved = 0;

    @NotBlank(message = "Warehouse location is required")
    @Size(max = 100, message = "Warehouse location cannot exceed 100 characters")
    private String warehouseLocation;
    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold = 10; // Default threshold



}