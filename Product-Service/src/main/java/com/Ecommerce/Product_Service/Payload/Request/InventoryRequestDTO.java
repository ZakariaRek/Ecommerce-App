package com.Ecommerce.Product_Service.Payload.Request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class InventoryRequestDTO {

    private UUID id; // Optional for updates

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull(message = "Availability status is required")
    private Boolean available;

    @Min(value = 0, message = "Reserved quantity cannot be negative")
    private Integer reserved = 0;

    @NotBlank(message = "Warehouse location is required")
    @Size(max = 100, message = "Warehouse location cannot exceed 100 characters")
    private String warehouseLocation;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold = 10; // Default threshold

    // Additional validation method
    @AssertTrue(message = "Available quantity cannot be less than reserved quantity")
    public boolean isQuantityValid() {
        if (quantity == null || reserved == null) {
            return true; // Let @NotNull handle null validation
        }
        return quantity >= reserved;
    }
}