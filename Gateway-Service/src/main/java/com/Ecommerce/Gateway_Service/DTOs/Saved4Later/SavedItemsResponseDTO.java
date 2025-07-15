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
public class SavedItemsResponseDTO {
    private UUID userId;
    private List<SavedItemDTO> items;
    private Integer itemCount;
    private LocalDateTime lastUpdated;
}
