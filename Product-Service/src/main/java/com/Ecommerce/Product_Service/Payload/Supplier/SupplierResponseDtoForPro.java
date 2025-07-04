package com.Ecommerce.Product_Service.Payload.Supplier;

import lombok.Data;

import java.util.UUID;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
@Data

public class SupplierResponseDtoForPro {
private UUID id;
    private String name;
    private String contactInfo;
    private String address;
    private Map<String, Object> contractDetails;
    private BigDecimal rating;
    private LocalDateTime createdAt;
}