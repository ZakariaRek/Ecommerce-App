package com.Ecommerce.Cart.Service.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event fired when an item is saved for later
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSavedEvent implements Serializable {
    private UUID userId;
    private UUID productId;
    private LocalDateTime timestamp;
}
