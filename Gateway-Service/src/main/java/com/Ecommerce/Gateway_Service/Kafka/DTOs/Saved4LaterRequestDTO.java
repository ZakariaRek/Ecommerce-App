package com.Ecommerce.Gateway_Service.Kafka.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Saved4LaterRequestDTO {
    private String correlationId;
    private String userId;
    private long timestamp;
}

