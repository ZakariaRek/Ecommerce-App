package com.Ecommerce.Product_Service.Payload;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Payload.Product.InventoryRequestDTO;
import com.Ecommerce.Product_Service.Payload.Product.InventoryRequestDTO;
import com.Ecommerce.Product_Service.Payload.Product.InventorySummaryDTO;
import com.Ecommerce.Product_Service.Payload.Inventory.InventoryResponseDTO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InventoryMapper {

    /**
     * Convert InventoryRequestDTO to Inventory entity
     */
    public Inventory toEntity(@Valid InventoryRequestDTO dto) {
        if (dto == null) return null;

        Inventory inventory = new Inventory();

        if (dto.getId() != null) {
            inventory.setId(dto.getId());
        }

        if (dto.getProductId() != null) {
            Product product = new Product();
            product.setId(dto.getProductId());
            inventory.setProduct(product);
        }

        inventory.setQuantity(dto.getQuantity());
        inventory.setWarehouseLocation(dto.getWarehouseLocation());

        if (dto.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(dto.getLowStockThreshold());
        }

        return inventory;
    }

    /**
     * Convert Inventory entity to InventoryResponseDTO
     */
    public com.Ecommerce.Product_Service.Payload.Inventory.InventoryResponseDTO toResponseDTO(Inventory entity) {
        if (entity == null) return null;

        InventoryResponseDTO dto = new InventoryResponseDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct() != null ? entity.getProduct().getId() : null);
        dto.setQuantity(entity.getQuantity());
        dto.setAvailable(entity.checkAvailability());
        dto.setWarehouseLocation(entity.getWarehouseLocation());
        dto.setReserved(0); // You might want to add this field to the entity
        dto.setLastUpdated(entity.getLastRestocked());

        return dto;
    }

    /**
     * Convert Inventory entity to InventorySummaryDTO (for listings)
     */
    public InventorySummaryDTO toSummaryDTO(Inventory entity) {
        if (entity == null) return null;

        InventorySummaryDTO dto = new InventorySummaryDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct() != null ? entity.getProduct().getId() : null);
        dto.setProductName(entity.getProduct() != null ? entity.getProduct().getName() : null);
        dto.setProductSku(entity.getProduct() != null ? entity.getProduct().getSku() : null);
        dto.setQuantity(entity.getQuantity());
        dto.setAvailable(entity.checkAvailability());
        dto.setWarehouseLocation(entity.getWarehouseLocation());
        dto.setLowStockThreshold(entity.getLowStockThreshold());
        dto.setIsLowStock(entity.getQuantity() <= entity.getLowStockThreshold());
        dto.setLastRestocked(entity.getLastRestocked());

        return dto;
    }

    /**
     * Update existing Inventory entity from InventoryRequestDTO
     */
    public void updateEntityFromDTO(Inventory entity, InventoryRequestDTO dto) {
        if (entity == null || dto == null) return;

        if (dto.getQuantity() != null) {
            entity.setQuantity(dto.getQuantity());
            entity.setLastRestocked(LocalDateTime.now());
        }

        if (dto.getWarehouseLocation() != null) {
            entity.setWarehouseLocation(dto.getWarehouseLocation());
        }

        if (dto.getLowStockThreshold() != null) {
            entity.setLowStockThreshold(dto.getLowStockThreshold());
        }
    }

    /**
     * Convert list of Inventory entities to list of InventoryResponseDTOs
     */
    public List<InventoryResponseDTO> toResponseDTOList(List<Inventory> entities) {
        if (entities == null) return null;

        return entities.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Inventory entities to list of InventorySummaryDTOs
     */
    public List<InventorySummaryDTO> toSummaryDTOList(List<Inventory> entities) {
        if (entities == null) return null;

        return entities.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a minimal Inventory entity for product association
     */
    public Inventory createMinimalInventory(Product product, String warehouseLocation) {
        Inventory inventory = new Inventory();
        inventory.setId(product.getId());
        inventory.setProduct(product);
        inventory.setQuantity(0);
        inventory.setLowStockThreshold(10);
        inventory.setWarehouseLocation(warehouseLocation != null ? warehouseLocation : "MAIN_WAREHOUSE");
        inventory.setLastRestocked(LocalDateTime.now());

        return inventory;
    }
}