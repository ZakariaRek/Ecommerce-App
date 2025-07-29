package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Inventory;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Payload.Inventory.InventoryUpdateRequest;
import com.Ecommerce.Product_Service.Repositories.InventoryRepository;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Services.Kakfa.InventoryEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Inventory Service Tests")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryEventService inventoryEventService;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory testInventory;
    private Product testProduct;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        testProduct = createTestProduct();
        testInventory = createTestInventory();
    }

    @Test
    @DisplayName("Should find all inventory")
    void findAllInventory_ShouldReturnAllInventory() {
        // Given
        List<Inventory> expectedInventory = Arrays.asList(testInventory);
        when(inventoryRepository.findAll()).thenReturn(expectedInventory);

        // When
        List<Inventory> result = inventoryService.findAllInventory();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactlyElementsOf(expectedInventory);
        verify(inventoryRepository).findAll();
    }

    @Test
    @DisplayName("Should find inventory by product ID")
    void findInventoryByProductId_WhenInventoryExists_ShouldReturnInventory() {
        // Given
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));

        // When
        Optional<Inventory> result = inventoryService.findInventoryByProductId(productId);

        // Then
        assertTrue(result.isPresent());
        assertThat(result.get()).isEqualTo(testInventory);
        verify(inventoryRepository).findById(productId);
    }

    @Test
    @DisplayName("Should check if inventory exists for product")
    void inventoryExistsForProduct_ShouldReturnCorrectBoolean() {
        // Given
        when(inventoryRepository.existsById(productId)).thenReturn(true);

        // When
        boolean exists = inventoryService.inventoryExistsForProduct(productId);

        // Then
        assertTrue(exists);
        verify(inventoryRepository).existsById(productId);
    }

    @Test
    @DisplayName("Should create inventory for product")
    void createInventoryForProduct_WithValidData_ShouldCreateInventory() {
        // Given
        when(inventoryRepository.existsById(productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Inventory result = inventoryService.createInventoryForProduct(
                productId, 100, "MAIN_WAREHOUSE", 10);

        // Then
        assertThat(result).isNotNull();
        verify(inventoryRepository).save(any(Inventory.class));
        verify(productRepository).save(any(Product.class));
        verify(inventoryEventService).publishInventoryCreatedEvent(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw exception when inventory already exists")
    void createInventoryForProduct_WhenInventoryExists_ShouldThrowException() {
        // Given
        when(inventoryRepository.existsById(productId)).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            inventoryService.createInventoryForProduct(productId, 100, "MAIN_WAREHOUSE", 10);
        });

        assertThat(exception.getMessage()).contains("Inventory already exists");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void createInventoryForProduct_WhenProductNotExists_ShouldThrowException() {
        // Given
        when(inventoryRepository.existsById(productId)).thenReturn(false);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.createInventoryForProduct(productId, 100, "MAIN_WAREHOUSE", 10);
        });

        assertThat(exception.getMessage()).contains("Product not found");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should update inventory successfully")
    void updateInventory_WithValidRequest_ShouldUpdateInventory() {
        // Given
        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest();
        updateRequest.setQuantity(150);
        updateRequest.setWarehouseLocation("WAREHOUSE_B");
        updateRequest.setLowStockThreshold(15);

        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Optional<Inventory> result = inventoryService.updateInventory(productId, updateRequest);

        // Then
        assertTrue(result.isPresent());
        verify(inventoryRepository).save(any(Inventory.class));
        verify(inventoryEventService).publishInventoryUpdatedEvent(any(Inventory.class));
    }

    @Test
    @DisplayName("Should update stock quantity")
    void updateStock_WithValidQuantity_ShouldUpdateStock() {
        // Given
        Integer newQuantity = 200;
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Optional<Inventory> result = inventoryService.updateStock(productId, newQuantity);

        // Then
        assertTrue(result.isPresent());
        verify(inventoryRepository).save(any(Inventory.class));
        verify(inventoryEventService).publishInventoryStockChangedEvent(any(Inventory.class), anyInt());
    }

    @Test
    @DisplayName("Should throw exception for negative stock quantity")
    void updateStock_WithNegativeQuantity_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.updateStock(productId, -10);
        });

        assertThat(exception.getMessage()).contains("Stock quantity cannot be negative");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should restock inventory")
    void restockInventory_WithValidQuantity_ShouldAddToStock() {
        // Given
        Integer additionalQuantity = 50;
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Optional<Inventory> result = inventoryService.restockInventory(productId, additionalQuantity);

        // Then
        assertTrue(result.isPresent());
        verify(inventoryRepository).save(any(Inventory.class));
        verify(inventoryEventService).publishInventoryRestockedEvent(
                any(Inventory.class), anyInt(), eq(additionalQuantity));
    }

    @Test
    @DisplayName("Should throw exception for invalid restock quantity")
    void restockInventory_WithZeroOrNegativeQuantity_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.restockInventory(productId, 0);
        });

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.restockInventory(productId, -10);
        });

        assertThat(exception1.getMessage()).contains("Additional quantity must be positive");
        assertThat(exception2.getMessage()).contains("Additional quantity must be positive");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should check if order can be fulfilled")
    void canFulfillOrder_WithSufficientStock_ShouldReturnTrue() {
        // Given
        testInventory.setQuantity(100);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));

        // When
        boolean canFulfill = inventoryService.canFulfillOrder(productId, 50);

        // Then
        assertTrue(canFulfill);
    }

    @Test
    @DisplayName("Should return false when insufficient stock")
    void canFulfillOrder_WithInsufficientStock_ShouldReturnFalse() {
        // Given
        testInventory.setQuantity(30);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));

        // When
        boolean canFulfill = inventoryService.canFulfillOrder(productId, 50);

        // Then
        assertFalse(canFulfill);
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void reserveStock_WithSufficientStock_ShouldReserveStock() {
        // Given
        testInventory.setQuantity(100);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        boolean reserved = inventoryService.reserveStock(productId, 30);

        // Then
        assertTrue(reserved);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should fail to reserve stock when insufficient")
    void reserveStock_WithInsufficientStock_ShouldReturnFalse() {
        // Given
        testInventory.setQuantity(20);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));

        // When
        boolean reserved = inventoryService.reserveStock(productId, 30);

        // Then
        assertFalse(reserved);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should release reserved stock")
    void releaseReservedStock_ShouldAddBackToStock() {
        // Given
        testInventory.setQuantity(70);  // Assuming 30 was reserved from 100
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        // When
        boolean released = inventoryService.releaseReservedStock(productId, 30);

        // Then
        assertTrue(released);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should delete inventory when no stock remains")
    void deleteInventory_WithZeroStock_ShouldDeleteInventory() {
        // Given
        testInventory.setQuantity(0);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));

        // When
        boolean deleted = inventoryService.deleteInventory(productId);

        // Then
        assertTrue(deleted);
        verify(inventoryEventService).publishInventoryDeletedEvent(testInventory);
        verify(inventoryRepository).deleteById(productId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting inventory with stock")
    void deleteInventory_WithRemainingStock_ShouldThrowException() {
        // Given
        testInventory.setQuantity(50);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(testInventory));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            inventoryService.deleteInventory(productId);
        });

        assertThat(exception.getMessage()).contains("Cannot delete inventory with remaining stock");
        verify(inventoryRepository, never()).deleteById(productId);
    }

    @Test
    @DisplayName("Should find low stock items")
    void findLowStockItems_ShouldReturnItemsBelowThreshold() {
        // Given
        List<Inventory> lowStockItems = Arrays.asList(testInventory);
        when(inventoryRepository.findByQuantityLessThanAndProduct_Status(eq(0), eq(ProductStatus.ACTIVE)))
                .thenReturn(lowStockItems);

        // When
        List<Inventory> result = inventoryService.findLowStockItems();

        // Then
        // Note: The actual filtering happens in the service method
        verify(inventoryRepository).findByQuantityLessThanAndProduct_Status(0, ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find inventory by warehouse location")
    void findByWarehouseLocation_ShouldReturnFilteredInventory() {
        // Given
        String location = "MAIN_WAREHOUSE";
        List<Inventory> allInventory = Arrays.asList(testInventory);
        testInventory.setWarehouseLocation(location);
        when(inventoryRepository.findAll()).thenReturn(allInventory);

        // When
        List<Inventory> result = inventoryService.findByWarehouseLocation(location);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarehouseLocation()).isEqualTo(location);
    }

    private Product createTestProduct() {
        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setStatus(ProductStatus.ACTIVE);
        product.setStock(100);
        return product;
    }

    private Inventory createTestInventory() {
        Inventory inventory = new Inventory();
        inventory.setId(productId);
        inventory.setProduct(testProduct);
        inventory.setQuantity(100);
        inventory.setLowStockThreshold(10);
        inventory.setWarehouseLocation("MAIN_WAREHOUSE");
        inventory.setLastRestocked(LocalDateTime.now());
        return inventory;
    }
}