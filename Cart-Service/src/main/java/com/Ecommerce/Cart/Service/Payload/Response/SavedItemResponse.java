package com.Ecommerce.Cart.Service.Payload.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedItemResponse {
    private UUID id;
    private UUID productId;
    private LocalDateTime savedAt;
}