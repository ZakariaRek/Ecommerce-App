package com.Ecommerce.Gateway_Service.Kafka.DTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchRequestEventDTO {
    private String correlationId;
    private List<UUID> productIds;
    private long timestamp;
}