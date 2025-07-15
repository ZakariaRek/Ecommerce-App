package com.Ecommerce.Cart.Service.Payload.kafka;

import com.Ecommerce.Cart.Service.Lisiteners.AsyncComm.Saved4LaterKafkaEventHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Saved4LaterRequestDTO {
    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("timestamp")
    private Long timestamp;
}
