package com.Ecommerce.Product_Service.Payload.inventory;

import lombok.Data;

import java.util.UUID;
@Data
public class InventoryRequestDTO {
    private UUID id;
    private UUID productId;
    private int quantity;
    private boolean available;
    private int reserved;
    private String warehouseLocation;


}
