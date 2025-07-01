package com.Ecommerce.Product_Service.Payload.Supplier;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ContractDetailsDTO {
    private String contractType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal contractValue;
    private String paymentTerms;
    private String deliveryTerms;
    private String warrantyPeriod;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;
    private String notes;
}