package com.Ecommerce.Cart.Service.Payload.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestCartRequest {
    @Valid
    @NotNull
    private List<GuestCartItem> items;

    private String sessionId;
}