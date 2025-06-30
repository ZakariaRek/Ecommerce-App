package com.Ecommerce.Product_Service.Payload.Supplier;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
public class SupplierSummaryDTO {
    private UUID id;
    private String name;
    private String contactInfo;
    private String address;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private Integer totalProducts;
}
