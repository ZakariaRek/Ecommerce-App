package com.Ecommerce.Product_Service.Payload.Kafka;



import com.Ecommerce.Product_Service.Entities.Category;
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
public class ProductBatchInfoDTO {
    private UUID id;
    private String name;
    private String imagePath;
    private Boolean inStock;
    private Integer availableQuantity;
    private ProductStatus status;
    private BigDecimal price;
    private BigDecimal discountValue;
    private String discountType;
}