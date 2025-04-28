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
 * Event fired when checkout process is initiated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutEvent implements Serializable {
    private UUID userId;
    private UUID cartId;
    private BigDecimal cartTotal;
    private int itemCount;
    private LocalDateTime timestamp;
}
