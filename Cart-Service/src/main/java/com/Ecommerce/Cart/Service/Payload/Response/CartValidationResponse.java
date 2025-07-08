package com.Ecommerce.Cart.Service.Payload.Response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartValidationResponse {
    private List<CartValidationItem> items;
    private BigDecimal totalPriceChange;
    private boolean hasChanges;
    private String message;
    private ConflictInfo conflictInfo;
}