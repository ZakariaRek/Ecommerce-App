package com.Ecommerce.Product_Service.Payload.Kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBatchResponseDTO {
    private String correlationId;
    private boolean success;
    private String message;
    private List<ProductBatchInfoDTO> products;
    private long timestamp;
}