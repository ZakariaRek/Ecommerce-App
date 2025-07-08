package com.Ecommerce.Cart.Service.Payload.Response;


import com.Ecommerce.Cart.Service.Payload.Request.ConflictResolutionStrategy;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictInfo {
    private List<CartConflict> conflicts;
    private ConflictResolutionStrategy recommendedStrategy;
}