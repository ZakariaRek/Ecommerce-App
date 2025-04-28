package com.Ecommerce.Cart.Service.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event fired when cart is updated (item added/removed/quantity changed)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartUpdatedEvent implements Serializable {
    private UUID userId;
    private UUID cartId;
    private String action; // "ADDED", "REMOVED", "UPDATED"
    private UUID productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal cartTotal;
    private LocalDateTime timestamp;
}

