package com.Ecommerce.Gateway_Service.Kafka.DTOs;

import com.Ecommerce.Gateway_Service.DTOs.Cart.CartServiceResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDTO {
    private String correlationId;
    private boolean success;
    private String message;
    private CartServiceResponseDTO data;
    private long timestamp;
}
