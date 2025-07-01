package com.Ecommerce.Product_Service.Payload.Supplier;


import com.Ecommerce.Product_Service.Payload.Product.ProductSummaryDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class SupplierResponseDTO {
    private UUID id;
    private String name;
    private String contactInfo;
    private String address;
    private Map<String, Object> contractDetails;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private Integer totalProducts;
    private List<ProductSummaryDTO> products;
}