package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Repositories.InventoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    public List<Inventory> findAllInventory() {
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> findInventoryByProductId(UUID productId) {
        return inventoryRepository.findById(productId);
    }

    @Transactional
    public Inventory updateInventory(Inventory inventory) {
        // Check if quantity is below threshold
        if (inventory.getQuantity() <= inventory.getLowStockThreshold()) {
            inventory.notifyLowStock();
        }

        // Update product status if necessary
        if (inventory.getQuantity() == 0) {
            inventory.getProduct().setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (inventory.getProduct().getStatus() == ProductStatus.OUT_OF_STOCK) {
            inventory.getProduct().setStatus(ProductStatus.ACTIVE);
        }

        return inventoryRepository.save(inventory);
    }
}