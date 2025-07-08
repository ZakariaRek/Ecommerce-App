package com.Ecommerce.Cart.Service.Payload.Request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateItem {
    @NotNull
    private UUID productId;

    @NotNull
    private BulkOperation operation;

    private Integer quantity;
    private BigDecimal price;
}