package com.Ecommerce.Cart.Service.Payload.Response;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Payload.Request.LocalStorageItem;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartConflict {
    private UUID productId;
    private String productName;
    private CartItem serverItem;
    private LocalStorageItem localItem;
    private String conflictType; // "QUANTITY_MISMATCH", "PRICE_DIFFERENCE"
}