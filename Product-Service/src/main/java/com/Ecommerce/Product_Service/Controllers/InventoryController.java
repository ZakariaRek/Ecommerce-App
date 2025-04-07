package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Services.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.findAllInventory());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventoryByProductId(@PathVariable UUID productId) {
        return inventoryService.findInventoryByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<Inventory> updateInventory(@RequestBody Inventory inventory) {
        Inventory updatedInventory = inventoryService.updateInventory(inventory);
        return ResponseEntity.ok(updatedInventory);
    }
}