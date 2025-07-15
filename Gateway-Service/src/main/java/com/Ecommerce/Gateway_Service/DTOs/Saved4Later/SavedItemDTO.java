package com.Ecommerce.Gateway_Service.DTOs.Saved4Later;


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
public class SavedItemDTO {
    private UUID id;
    private UUID userId;
    private UUID productId;
    private LocalDateTime savedAt;
}


