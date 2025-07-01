package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Payload.Inventory.InventoryUpdateRequest;
import com.Ecommerce.Product_Service.Payload.InventoryMapper;
import com.Ecommerce.Product_Service.Payload.Product.InventoryRequestDTO;
import com.Ecommerce.Product_Service.Payload.Inventory.InventoryResponseDTO;
import com.Ecommerce.Product_Service.Payload.Product.InventorySummaryDTO;
import com.Ecommerce.Product_Service.Services.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryMapper inventoryMapper;

    /**
     * Create new inventory record - FIXED VERSION
     */
    @PostMapping
    public ResponseEntity<?> createInventory(@Valid @RequestBody InventoryRequestDTO requestDTO) {
        try {
            // Check if inventory already exists
            if (inventoryService.inventoryExistsForProduct(requestDTO.getProductId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "error", "INVENTORY_ALREADY_EXISTS",
                                "message", "Inventory already exists for product ID: " + requestDTO.getProductId(),
                                "timestamp", LocalDateTime.now()
                        ));
            }

            // Use the safer creation method
            Inventory savedInventory = inventoryService.createInventoryForProduct(
                    requestDTO.getProductId(),
                    requestDTO.getQuantity(),
                    requestDTO.getWarehouseLocation(),
                    requestDTO.getLowStockThreshold()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(inventoryMapper.toResponseDTO(savedInventory));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "INVALID_INPUT",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "CONFLICT",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        } catch (Exception e) {
            // Log the full error for debugging
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "INTERNAL_ERROR",
                            "message", "Failed to create inventory: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Alternative: Create inventory using simplified payload
     */
    @PostMapping("/simple")
    public ResponseEntity<?> createInventorySimple(
            @RequestParam UUID productId,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "MAIN_WAREHOUSE") String warehouseLocation,
            @RequestParam(defaultValue = "10") Integer lowStockThreshold)
    {

        try {
            if (inventoryService.inventoryExistsForProduct(productId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Inventory already exists for this product"));
            }

            Inventory savedInventory = inventoryService.createInventoryForProduct(
                    productId, quantity, warehouseLocation, lowStockThreshold
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(inventoryMapper.toResponseDTO(savedInventory));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if inventory exists for product
     */
    @GetMapping("/{productId}/exists")
    public ResponseEntity<Map<String, Boolean>> checkInventoryExists(@PathVariable UUID productId) {
        boolean exists = inventoryService.inventoryExistsForProduct(productId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Get all inventory records
     */
    @GetMapping
    public ResponseEntity<Map<String, List<InventoryResponseDTO>>> getAllInventory() {
        List<Inventory> inventoryList = inventoryService.findAllInventory();
        Map<String, List<InventoryResponseDTO>> inventoryMap = inventoryList.stream()
                .map(inventory -> {
                    InventoryResponseDTO responseDTO = new InventoryResponseDTO();
                    responseDTO.setId(inventory.getId());
                    responseDTO.setProductId(inventory.getProduct().getId());
                    responseDTO.setQuantity(inventory.getQuantity());
                    responseDTO.setAvailable(inventory.getQuantity() > 0);
                    responseDTO.setWarehouseLocation(inventory.getWarehouseLocation());
                    responseDTO.setLastUpdated(inventory.getLastRestocked());
                    responseDTO.setProductName(inventory.getProduct().getName());
                    responseDTO.setProductSku(inventory.getProduct().getSku());
                    responseDTO.setLowStockThreshold(inventory.getLowStockThreshold());
                    responseDTO.setIsLowStock(responseDTO.getIsLowStock());
                    responseDTO.setIsOutOfStock(responseDTO.getIsOutOfStock());
                    return responseDTO;
                })
                .collect(Collectors.groupingBy(InventoryResponseDTO::getProductName));
        return ResponseEntity.ok(inventoryMap);
    }
    /**
     * Get inventory by product ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(@PathVariable UUID productId) {
        return inventoryService.findInventoryByProductId(productId)
                .map(inventory -> {
                    InventoryResponseDTO responseDTO = new InventoryResponseDTO();
                    responseDTO.setId(inventory.getId());
                    responseDTO.setProductId(inventory.getProduct().getId());
                    responseDTO.setQuantity(inventory.getQuantity());
                    responseDTO.setAvailable(inventory.getQuantity() > 0);
                    responseDTO.setWarehouseLocation(inventory.getWarehouseLocation());
//                    responseDTO.setReserved(inventory.getReserved());
                    responseDTO.setLastUpdated(inventory.getLastRestocked());
                    responseDTO.setProductName(inventory.getProduct().getName());
                    responseDTO.setProductSku(inventory.getProduct().getSku());
                    responseDTO.setLowStockThreshold(inventory.getLowStockThreshold());
                    responseDTO.setIsLowStock(inventory.getQuantity() <= inventory.getLowStockThreshold());
                    responseDTO.setIsOutOfStock(inventory.getQuantity() == 0);
//                    responseDTO.setStockStatus(inventory.getQuantity() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
//                    responseDTO.setAvailableQuantity(inventory.getQuantity() - inventory.getReserved());
                    return responseDTO;
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    /**
     * Update existing inventory
     */
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryUpdateRequest requestDTO) {
        try {

//            Inventory inventory = inventoryMapper.toEntity(requestDTO);
//            inventory.setId(productId);

            return inventoryService.updateInventory(productId, requestDTO)
                    .map(inventoryMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update stock quantity only
     */
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<?> updateStock(
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {
        try {
            return inventoryService.updateStock(productId, quantity)
                    .map(inventoryMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Restock inventory (add to existing quantity)
     */
    @PostMapping("/{productId}/restock")
    public ResponseEntity<?> restockInventory(
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {
        try {
            return inventoryService.restockInventory(productId, quantity)
                    .map(inventoryMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete inventory record
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteInventory(@PathVariable UUID productId) {
        try {
            boolean deleted = inventoryService.deleteInventory(productId);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get low stock items
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventorySummaryDTO>> getLowStockItems() {
        List<InventorySummaryDTO> lowStockItems = inventoryMapper.toSummaryDTOList(
                inventoryService.findLowStockItems()
        );
        return ResponseEntity.ok(lowStockItems);
    }

    /**
     * Get inventory by warehouse location
     */
    @GetMapping("/warehouse/{location}")
    public ResponseEntity<List<InventoryResponseDTO>> getInventoryByWarehouse(@PathVariable String location) {
        List<InventoryResponseDTO> inventoryList = inventoryMapper.toResponseDTOList(
                inventoryService.findByWarehouseLocation(location)
        );
        return ResponseEntity.ok(inventoryList);
    }

    /**
     * Check if product can fulfill order
     */
    @GetMapping("/{productId}/can-fulfill")
    public ResponseEntity<Map<String, Object>> canFulfillOrder(
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {

        boolean canFulfill = inventoryService.canFulfillOrder(productId, quantity);
        Optional<Inventory> inventoryOpt = inventoryService.findInventoryByProductId(productId);

        Map<String, Object> response = new HashMap<>();
        response.put("canFulfill", canFulfill);
        response.put("requestedQuantity", quantity);

        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            response.put("availableQuantity", inventory.getQuantity());
            response.put("shortfall", Math.max(0, quantity - inventory.getQuantity()));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Reserve stock for order
     */
    @PostMapping("/{productId}/reserve")
    public ResponseEntity<?> reserveStock(
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {
        try {
            boolean reserved = inventoryService.reserveStock(productId, quantity);
            if (reserved) {
                return inventoryService.findInventoryByProductId(productId)
                        .map(inventoryMapper::toResponseDTO)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Insufficient stock to reserve " + quantity + " units"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Release reserved stock
     */
    @PostMapping("/{productId}/release")
    public ResponseEntity<?> releaseReservedStock(
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {
        try {
            boolean released = inventoryService.releaseReservedStock(productId, quantity);
            if (released) {
                return inventoryService.findInventoryByProductId(productId)
                        .map(inventoryMapper::toResponseDTO)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Failed to release stock"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}