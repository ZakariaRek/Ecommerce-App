package com.Ecommerce.Product_Service.Payload.Product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductBatchRequestDTO {

    @NotEmpty(message = "Product IDs list cannot be empty")
    @Size(max = 100, message = "Cannot request more than 100 products at once")
    private List<UUID> productIds;
}