package com.Ecommerce.Cart.Service.Payload.kafka;

import com.Ecommerce.Cart.Service.Payload.Response.ShoppingCartResponse;
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
    private ShoppingCartResponse data;
    private long timestamp;
}