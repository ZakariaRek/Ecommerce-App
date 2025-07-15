package com.Ecommerce.Product_Service.Payload.Product;

import com.Ecommerce.Product_Service.Entities.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchResponseDTO {
    private UUID id;
    private String name;
    private BigDecimal price;
    private String imagePath;  // First image or default
    private Boolean inStock;
    private Integer availableQuantity;
    private ProductStatus status;
    private BigDecimal discountValue;
    private String discountType; // e.g., "PERCENTAGE", "AMOUNT"
}