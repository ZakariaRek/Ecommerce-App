package com.Ecommerce.Gateway_Service.Kafka.DTOs;

import com.Ecommerce.Gateway_Service.DTOs.Saved4Later.SavedItemsResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Saved4LaterResponseDTO {
    private String correlationId;
    private boolean success;
    private String message;
    private SavedItemsResponseDTO data;
    private long timestamp;
}
