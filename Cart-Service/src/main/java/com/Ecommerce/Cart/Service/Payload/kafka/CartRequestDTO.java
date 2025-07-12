package com.Ecommerce.Cart.Service.Payload.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRequestDTO {
    @JsonProperty("userId")
    private String userId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("timestamp")
    private Long timestamp;
}