package com.Ecommerce.Cart.Service.Payload.Request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkSaveForLaterRequest {

    @NotNull(message = "Product IDs list is required")
    @NotEmpty(message = "At least one product ID is required")
    private List<UUID> productIds;
}