package com.Ecommerce.Product_Service.Payload.Supplier;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class SupplierRequestDTO {

    @NotBlank(message = "Supplier name is required")
    @Size(min = 2, max = 100, message = "Supplier name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Contact info is required")
    @Size(max = 500, message = "Contact info cannot exceed 500 characters")
    private String contactInfo;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    private Map<String, Object> contractDetails;

    @DecimalMin(value = "0.0", message = "Rating cannot be negative")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    private BigDecimal rating;

    @Size(max = 100, message = "Cannot have more than 100 products")
    private List<UUID> productIds;
}