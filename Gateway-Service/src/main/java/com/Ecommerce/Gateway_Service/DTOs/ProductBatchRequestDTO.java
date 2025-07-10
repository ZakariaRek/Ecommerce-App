package com.Ecommerce.Gateway_Service.DTOs;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductBatchRequestDTO {
    private List<UUID> productIds;
}