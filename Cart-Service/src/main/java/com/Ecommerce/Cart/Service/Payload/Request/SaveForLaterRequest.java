package com.Ecommerce.Cart.Service.Payload.Request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveForLaterRequest {
    @NotNull(message = "Product ID is required")
    private UUID productId;
}
