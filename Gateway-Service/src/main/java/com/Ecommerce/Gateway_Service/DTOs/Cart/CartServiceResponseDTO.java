package com.Ecommerce.Gateway_Service.DTOs.Cart;

import lombok.Data;

@Data
public class CartServiceResponseDTO {
    private boolean success;
    private String message;
    private ShoppingCartDTO data;
}