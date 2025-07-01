package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Payload.Inventory.InventoryRequestDTO;
import com.Ecommerce.Product_Service.Repositories.InventoryRepository;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Services.Kakfa.InventoryEventService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryEventService inventoryEventService;

    // ====== READ OPERATIONS ======

    public List<Inventory> findAllInventory() {
        return inventoryRepository.findAll();
    }

    public Optional<Inventory> findInventoryByProductId(UUID productId) {
        return inventoryRepository.findById(productId);
    }

    public List<Inventory> findLowStockItems() {
        return inventoryRepository.findByQuantityLessThanAndProduct_Status(0, ProductStatus.ACTIVE)
                .stream()
                .filter(inventory -> inventory.getQuantity() <= inventory.getLowStockThreshold())
                .toList();
    }

    public List<Inventory> findByWarehouseLocation(String location) {
        return inventoryRepository.findAll()
                .stream()
                .filter(inventory -> location.equals(inventory.getWarehouseLocation()))
                .toList();
    }

    public List<Inventory> findItemsNeedingRestock(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return inventoryRepository.findByLastRestockedBefore(cutoffDate);
    }

    // ====== UTILITY METHODS ======

    public boolean inventoryExistsForProduct(UUID productId) {
        return inventoryRepository.existsById(productId);
    }

    public boolean isInStock(UUID productId) {
        return inventoryRepository.findById(productId)
                .map(inventory -> inventory.getQuantity() > 0)
                .orElse(false);
    }

    public boolean canFulfillOrder(UUID productId, Integer requestedQuantity) {
        return inventoryRepository.findById(productId)
                .map(inventory -> inventory.getQuantity() >= requestedQuantity)
                .orElse(false);
    }

    // ====== CREATE OPERATIONS ======

    @Transactional
    public Inventory createInventory(Inventory inventory) {
        // Validate that product exists
        if (inventory.getProduct() == null || inventory.getProduct().getId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        UUID productId = inventory.getProduct().getId();

        // Check if inventory already exists for this product
        if (inventoryRepository.existsById(productId)) {
            throw new IllegalStateException("Inventory already exists for product ID: " + productId);
        }

        // Fetch the complete product entity from database
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }

        Product product = productOpt.get();

        // Create NEW inventory entity (important: don't set ID yet)
        Inventory newInventory = new Inventory();

        // Set the product first (this will generate the ID through @MapsId)
        newInventory.setProduct(product);

        // Now set other fields
        newInventory.setQuantity(inventory.getQuantity() != null ? inventory.getQuantity() : 0);
        newInventory.setLowStockThreshold(inventory.getLowStockThreshold() != null ? inventory.getLowStockThreshold() : 10);
        newInventory.setWarehouseLocation(inventory.getWarehouseLocation());
        newInventory.setLastRestocked(LocalDateTime.now());

        // Update product status based on stock
        updateProductStatusBasedOnStock(product, newInventory.getQuantity());

        try {
            // Save inventory - use save() for new entities
            Inventory savedInventory = inventoryRepository.saveAndFlush(newInventory);

            // Update product with inventory reference and sync stock
            product.setInventory(savedInventory);
            product.setStock(newInventory.getQuantity());
            productRepository.save(product);

            // Publish event
            inventoryEventService.publishInventoryCreatedEvent(savedInventory);

            return savedInventory;

        } catch (Exception e) {
            // Log the error and re-throw with meaningful message
            throw new RuntimeException("Failed to create inventory for product " + productId + ": " + e.getMessage(), e);
        }
    }

    @Transactional
    public Inventory createInventoryForProduct(UUID productId, Integer quantity, String warehouseLocation, Integer lowStockThreshold) {
        // Check if inventory already exists
        if (inventoryRepository.existsById(productId)) {
            throw new IllegalStateException("Inventory already exists for product ID: " + productId);
        }

        // Fetch product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        // Create new inventory
        Inventory inventory = new Inventory();
        inventory.setProduct(product); // This sets the ID through @MapsId
        inventory.setQuantity(quantity != null ? quantity : 0);
        inventory.setLowStockThreshold(lowStockThreshold != null ? lowStockThreshold : 10);
        inventory.setWarehouseLocation(warehouseLocation != null ? warehouseLocation : "MAIN_WAREHOUSE");
        inventory.setLastRestocked(LocalDateTime.now());

        // Update product status
        updateProductStatusBasedOnStock(product, inventory.getQuantity());

        // Save
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Update product
        product.setInventory(savedInventory);
        product.setStock(inventory.getQuantity());
        productRepository.save(product);

        // Publish event
        inventoryEventService.publishInventoryCreatedEvent(savedInventory);

        return savedInventory;
    }

    // ====== UPDATE OPERATIONS ======

    @Transactional
    public Optional<Inventory> updateInventory(UUID productId, Inventory updatedInventory) {
        return inventoryRepository.findById(productId)
                .map(existingInventory -> {
                    Integer previousQuantity = existingInventory.getQuantity();
                    Integer previousThreshold = existingInventory.getLowStockThreshold();

                    // Update fields carefully
                    if (updatedInventory.getQuantity() != null) {
                        existingInventory.setQuantity(updatedInventory.getQuantity());
                        existingInventory.setLastRestocked(LocalDateTime.now());
                    }

                    if (updatedInventory.getLowStockThreshold() != null) {
                        existingInventory.setLowStockThreshold(updatedInventory.getLowStockThreshold());
                    }

                    if (updatedInventory.getWarehouseLocation() != null) {
                        existingInventory.setWarehouseLocation(updatedInventory.getWarehouseLocation());
                    }

                    // Update product status and stock
                    updateProductStatusBasedOnStock(existingInventory.getProduct(), existingInventory.getQuantity());
                    existingInventory.getProduct().setStock(existingInventory.getQuantity());
                    productRepository.save(existingInventory.getProduct());

                    // Check for low stock
                    checkAndNotifyLowStock(existingInventory);

                    // Save inventory
                    Inventory savedInventory = inventoryRepository.save(existingInventory);

                    // Publish events
                    inventoryEventService.publishInventoryUpdatedEvent(savedInventory);

                    if (!previousQuantity.equals(existingInventory.getQuantity())) {
                        inventoryEventService.publishInventoryStockChangedEvent(savedInventory, previousQuantity);
                    }

                    if (!previousThreshold.equals(existingInventory.getLowStockThreshold())) {
                        inventoryEventService.publishInventoryThresholdChangedEvent(savedInventory, previousThreshold);
                    }

                    return savedInventory;
                });
    }

    @Transactional
    public Optional<Inventory> partialUpdateInventory(UUID productId, InventoryRequestDTO requestDTO) {
        return inventoryRepository.findById(productId)
                .map(existingInventory -> {
                    Integer previousQuantity = existingInventory.getQuantity();
                    Integer previousThreshold = existingInventory.getLowStockThreshold();

                    // Update only provided fields
                    if (requestDTO.getQuantity() != 0 && requestDTO.getQuantity() >= 0) {
                        existingInventory.setQuantity(requestDTO.getQuantity());
                        existingInventory.setLastRestocked(LocalDateTime.now());
                    }

                    if (requestDTO.getWarehouseLocation() != null) {
                        existingInventory.setWarehouseLocation(requestDTO.getWarehouseLocation());
                    }



                    // Update product status and stock
                    updateProductStatusBasedOnStock(existingInventory.getProduct(), existingInventory.getQuantity());
                    existingInventory.getProduct().setStock(existingInventory.getQuantity());
                    productRepository.save(existingInventory.getProduct());

                    // Check for low stock
                    checkAndNotifyLowStock(existingInventory);

                    // Save inventory
                    Inventory savedInventory = inventoryRepository.save(existingInventory);

                    // Publish events
                    inventoryEventService.publishInventoryUpdatedEvent(savedInventory);

                    if (!previousQuantity.equals(existingInventory.getQuantity())) {
                        inventoryEventService.publishInventoryStockChangedEvent(savedInventory, previousQuantity);
                    }

                    if (previousThreshold != null && !previousThreshold.equals(existingInventory.getLowStockThreshold())) {
                        inventoryEventService.publishInventoryThresholdChangedEvent(savedInventory, previousThreshold);
                    }

                    return savedInventory;
                });
    }

    @Transactional
    public Optional<Inventory> updateStock(UUID productId, Integer newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        return inventoryRepository.findById(productId)
                .map(inventory -> {
                    Integer previousQuantity = inventory.getQuantity();
                    inventory.setQuantity(newQuantity);
                    inventory.setLastRestocked(LocalDateTime.now());

                    // Update product status and stock
                    updateProductStatusBasedOnStock(inventory.getProduct(), newQuantity);
                    inventory.getProduct().setStock(newQuantity);
                    productRepository.save(inventory.getProduct());

                    // Check for low stock
                    checkAndNotifyLowStock(inventory);

                    // Save inventory
                    Inventory savedInventory = inventoryRepository.save(inventory);

                    // Publish events
                    inventoryEventService.publishInventoryStockChangedEvent(savedInventory, previousQuantity);

                    return savedInventory;
                });
    }

    @Transactional
    public Optional<Inventory> updateLowStockThreshold(UUID productId, Integer newThreshold) {
        if (newThreshold < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative");
        }

        return inventoryRepository.findById(productId)
                .map(inventory -> {
                    Integer previousThreshold = inventory.getLowStockThreshold();
                    inventory.setLowStockThreshold(newThreshold);

                    // Check for low stock with new threshold
                    checkAndNotifyLowStock(inventory);

                    // Save inventory
                    Inventory savedInventory = inventoryRepository.save(inventory);

                    // Publish event
                    inventoryEventService.publishInventoryThresholdChangedEvent(savedInventory, previousThreshold);

                    return savedInventory;
                });
    }

    @Transactional
    public Optional<Inventory> restockInventory(UUID productId, Integer additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("Additional quantity must be positive");
        }

        return inventoryRepository.findById(productId)
                .map(inventory -> {
                    Integer previousQuantity = inventory.getQuantity();
                    Integer newQuantity = previousQuantity + additionalQuantity;

                    inventory.setQuantity(newQuantity);
                    inventory.setLastRestocked(LocalDateTime.now());

                    // Update product status and stock
                    updateProductStatusBasedOnStock(inventory.getProduct(), newQuantity);
                    inventory.getProduct().setStock(newQuantity);
                    productRepository.save(inventory.getProduct());

                    // Save inventory
                    Inventory savedInventory = inventoryRepository.save(inventory);

                    // Publish events
                    inventoryEventService.publishInventoryRestockedEvent(savedInventory, previousQuantity, additionalQuantity);

                    return savedInventory;
                });
    }

    // ====== STOCK RESERVATION OPERATIONS ======

    @Transactional
    public boolean reserveStock(UUID productId, Integer quantity) {
        return inventoryRepository.findById(productId)
                .map(inventory -> {
                    if (inventory.getQuantity() >= quantity) {
                        Integer newQuantity = inventory.getQuantity() - quantity;
                        inventory.setQuantity(newQuantity);

                        updateProductStatusBasedOnStock(inventory.getProduct(), newQuantity);
                        checkAndNotifyLowStock(inventory);

                        inventoryRepository.save(inventory);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Transactional
    public boolean releaseReservedStock(UUID productId, Integer quantity) {
        return inventoryRepository.findById(productId)
                .map(inventory -> {
                    Integer newQuantity = inventory.getQuantity() + quantity;
                    inventory.setQuantity(newQuantity);

                    updateProductStatusBasedOnStock(inventory.getProduct(), newQuantity);

                    inventoryRepository.save(inventory);
                    return true;
                })
                .orElse(false);
    }

    // ====== DELETE OPERATIONS ======

    @Transactional
    public boolean deleteInventory(UUID productId) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findById(productId);

        if (inventoryOpt.isEmpty()) {
            return false;
        }

        Inventory inventory = inventoryOpt.get();

        // Check if it's safe to delete
        if (inventory.getQuantity() > 0) {
            throw new IllegalStateException("Cannot delete inventory with remaining stock. Please transfer stock first.");
        }

        // Update product reference
        Product product = inventory.getProduct();
        if (product != null) {
            product.setInventory(null);
            product.setStock(0);
            product.setStatus(ProductStatus.INACTIVE);
            productRepository.save(product);
        }

        // Publish event before deletion
        inventoryEventService.publishInventoryDeletedEvent(inventory);

        // Delete inventory
        inventoryRepository.deleteById(productId);

        return true;
    }

    // ====== LEGACY SUPPORT METHOD ======

    @Transactional
    public Inventory updateInventory(Inventory inventory) {
        // Legacy method - kept for backward compatibility with existing ProductService
        Integer previousQuantity = null;

        if (inventory.getId() != null) {
            Optional<Inventory> existing = inventoryRepository.findById(inventory.getId());
            if (existing.isPresent()) {
                previousQuantity = existing.get().getQuantity();
            }
        }

        // Check if quantity is below threshold
        checkAndNotifyLowStock(inventory);

        // Update product status if necessary
        updateProductStatusBasedOnStock(inventory.getProduct(), inventory.getQuantity());

        // Save inventory
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Publish events
        if (previousQuantity != null && !previousQuantity.equals(inventory.getQuantity())) {
            inventoryEventService.publishInventoryStockChangedEvent(savedInventory, previousQuantity);
        }

        return savedInventory;
    }

    // ====== PRIVATE HELPER METHODS ======

    private void updateProductStatusBasedOnStock(Product product, Integer quantity) {
        if (product == null) return;

        if (quantity == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }
    }

    private void checkAndNotifyLowStock(Inventory inventory) {
        if (inventory.getQuantity() <= inventory.getLowStockThreshold()) {
            inventory.notifyLowStock();
            inventoryEventService.publishInventoryLowStockEvent(inventory);
        }
    }
}