package com.Ecommerce.Gateway_Service.DTOs.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOrderResponseDTO {
    private List<EnrichedOrderResponse> orders;
    private Map<String, String> failures; // orderId -> error message
    private int totalRequested;
    private int successful;
    private int failed;
    private boolean includeProducts;
    private long processingTimeMs;
}