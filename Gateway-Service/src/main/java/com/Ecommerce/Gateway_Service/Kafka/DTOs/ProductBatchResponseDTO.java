package com.Ecommerce.Gateway_Service.Kafka.DTOs;
import com.Ecommerce.Gateway_Service.DTOs.ProductBatchInfoDTO;
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
    private String message;
    private boolean success;
    private List<ProductBatchInfoDTO> products;
    private long timestamp;
}