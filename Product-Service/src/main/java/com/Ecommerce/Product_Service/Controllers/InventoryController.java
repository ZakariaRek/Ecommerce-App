package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Payload.inventory.InventoryRequestDTO;
import com.Ecommerce.Product_Service.Payload.inventory.InventoryResponseDTO;
import com.Ecommerce.Product_Service.Services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponseDTO>> getAllInventory() {
        List<InventoryResponseDTO> inventoryList = inventoryService.findAllInventory()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(inventoryList);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(@PathVariable UUID productId) {
        return inventoryService.findInventoryByProductId(productId)
                .map(this::convertToResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<InventoryResponseDTO> updateInventory(@RequestBody InventoryRequestDTO requestDTO) {
        Inventory inventory = convertToEntity(requestDTO);
        Inventory updatedInventory = inventoryService.updateInventory(inventory);
        return ResponseEntity.ok(convertToResponseDTO(updatedInventory));
    }

    private InventoryResponseDTO convertToResponseDTO(Inventory inventory) {
        InventoryResponseDTO dto = new InventoryResponseDTO();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProduct() != null ? inventory.getProduct().getId() : null);
        dto.setQuantity(inventory.getQuantity());
        dto.setAvailable(inventory.checkAvailability());
        dto.setWarehouseLocation(inventory.getWarehouseLocation());
        // Note: Assuming 'reserved' information is not in the entity, setting to 0
        dto.setReserved(0);
        dto.setLastUpdated(inventory.getLastRestocked());
        return dto;
    }

    private Inventory convertToEntity(InventoryRequestDTO dto) {
        Inventory inventory = new Inventory();
        if (dto.getId() != null) {
            inventory.setId(dto.getId());
        }

        Product product = new Product();
        product.setId(dto.getProductId());
        inventory.setProduct(product);

        inventory.setQuantity(dto.getQuantity());
        inventory.setWarehouseLocation(dto.getWarehouseLocation());
        // Other fields like lowStockThreshold might need to be set based on business logic

        return inventory;
    }
}