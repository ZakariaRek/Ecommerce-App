package com.Ecommerce.Gateway_Service.DTOs.Order;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOrderRequestDTO {
    private List<String> orderIds;
    private boolean includeProducts = true;
    private String userId; // Optional filter
    private String status; // Optional filter
}