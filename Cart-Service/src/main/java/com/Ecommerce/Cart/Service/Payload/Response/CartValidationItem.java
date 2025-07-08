package com.Ecommerce.Cart.Service.Payload.Response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartValidationItem {
    private UUID productId;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal currentPrice;
    private boolean priceChanged;
    private boolean availabilityChanged;
    private boolean inStock;
    private Integer maxQuantity;
    private String validationMessage;

    public boolean hasChanges() {
        return priceChanged || availabilityChanged;
    }
}