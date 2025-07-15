
package com.Ecommerce.Cart.Service.Payload.kafka;

import com.Ecommerce.Cart.Service.Lisiteners.AsyncComm.Saved4LaterKafkaEventHandler;
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
    private Saved4LaterKafkaEventHandler.SavedItemsDataDTO data;
    private long timestamp;
}
