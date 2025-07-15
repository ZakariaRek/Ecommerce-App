package com.Ecommerce.Gateway_Service.DTOs.Saved4Later;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedSavedItemsResponse {
    private UUID userId;
    private List<EnrichedSavedItemDTO> items;
    private Integer itemCount;
    private LocalDateTime lastUpdated;
    private Integer availableItemsCount; // How many saved items are still in stock
    private Integer unavailableItemsCount; // How many saved items are out of stock
}