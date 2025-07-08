package com.Ecommerce.Cart.Service.Payload.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSyncRequest {
    @Valid
    @NotNull
    private List<LocalStorageItem> items;

    @Builder.Default
    private ConflictResolutionStrategy conflictStrategy = ConflictResolutionStrategy.SUM_QUANTITIES;

    private LocalDateTime lastUpdated;
    private String deviceId;
    private String sessionId;
}