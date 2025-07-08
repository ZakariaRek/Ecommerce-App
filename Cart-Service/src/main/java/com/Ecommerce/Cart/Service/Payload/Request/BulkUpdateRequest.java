package com.Ecommerce.Cart.Service.Payload.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateRequest {
    @Valid
    @NotNull
    private List<BulkUpdateItem> items;

    private String sessionId;
}
