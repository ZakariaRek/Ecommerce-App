package com.Ecommerce.Gateway_Service.DTOs.Order;

import com.Ecommerce.Gateway_Service.DTOs.EnrichedOrderItemDTO;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class EnrichedOrderResponse {

    private UUID id;
    private UUID userId;
    private UUID cartId;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal discount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID billingAddressId;
    private UUID shippingAddressId;
    private List<EnrichedOrderItemDTO> items;

    // Calculated fields
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public int getTotalQuantity() {
        return items != null ?
                items.stream()
                        .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                        .sum() : 0;
    }
}