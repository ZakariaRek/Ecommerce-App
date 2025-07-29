package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Payload.Product.ProductBatchResponseDTO;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Services.Kakfa.ProductEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ProductEventService productEventService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private UUID testProductId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testProduct = createTestProduct();
    }

    @Test
    @DisplayName("Should find all products successfully")
    void findAllProducts_ShouldReturnAllProducts() {
        // Given
        List<Product> expectedProducts = Arrays.asList(testProduct, createTestProduct());
        when(productRepository.findAll()).thenReturn(expectedProducts);

        // When
        List<Product> actualProducts = productService.findAllProducts();

        // Then
        assertThat(actualProducts).hasSize(2);
        assertThat(actualProducts).containsExactlyElementsOf(expectedProducts);
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Should find product by ID successfully")
    void findProductById_WhenProductExists_ShouldReturnProduct() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        Optional<Product> result = productService.findProductById(testProductId);

        // Then
        assertTrue(result.isPresent());
        assertThat(result.get()).isEqualTo(testProduct);
        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should return empty when product not found")
    void findProductById_WhenProductNotExists_ShouldReturnEmpty() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.empty());

        // When
        Optional<Product> result = productService.findProductById(testProductId);

        // Then
        assertTrue(result.isEmpty());
        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should save product successfully")
    void saveProduct_ShouldSaveAndReturnProduct() {
        // Given
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        // When
        Product savedProduct = productService.saveProduct(testProduct);

        // Then
        assertThat(savedProduct).isEqualTo(testProduct);
        verify(productEventService).createProductCreatedEvent(testProduct);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should find products by status")
    void findProductsByStatus_ShouldReturnFilteredProducts() {
        // Given
        ProductStatus status = ProductStatus.ACTIVE;
        List<Product> expectedProducts = Arrays.asList(testProduct);
        when(productRepository.findByStatus(status)).thenReturn(expectedProducts);

        // When
        List<Product> result = productService.findProductsByStatus(status);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProduct);
        verify(productRepository).findByStatus(status);
    }

    @Test
    @DisplayName("Should delete product successfully")
    void deleteProduct_ShouldDeleteProduct() {
        // When
        productService.deleteProduct(testProductId);

        // Then
        verify(productEventService).createProductDeletedEvent(testProductId);
        verify(productRepository).deleteById(testProductId);
    }

    @Test
    @DisplayName("Should update product status successfully")
    void updateProductStatus_WhenProductExists_ShouldUpdateStatus() {
        // Given
        ProductStatus newStatus = ProductStatus.INACTIVE;
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Optional<Product> result = productService.updateProductStatus(testProductId, newStatus);

        // Then
        assertTrue(result.isPresent());
        assertThat(result.get().getStatus()).isEqualTo(newStatus);
        verify(productEventService).createStatusChangedEvent(testProduct, newStatus);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should get batch product info successfully")
    void getBatchProductInfo_ShouldReturnBatchResponse() {
        // Given
        List<UUID> productIds = Arrays.asList(testProductId);
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findAllById(productIds)).thenReturn(products);

        // When
        List<ProductBatchResponseDTO> result = productService.getBatchProductInfo(productIds);

        // Then
        assertThat(result).hasSize(1);
        ProductBatchResponseDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(testProduct.getId());
        assertThat(dto.getName()).isEqualTo(testProduct.getName());
        assertThat(dto.getPrice()).isEqualTo(testProduct.getPrice());
        verify(productRepository).findAllById(productIds);
    }

    @Test
    @DisplayName("Should get products without inventory")
    void getProductsWithoutInventory_ShouldReturnProducts() {
        // Given
        List<Product> expectedProducts = Arrays.asList(testProduct);
        when(productRepository.findByInventoryIsNull()).thenReturn(expectedProducts);

        // When
        List<Product> result = productService.getProductsWithoutInventory();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testProduct);
        verify(productRepository).findByInventoryIsNull();
    }

    @Test
    @DisplayName("Should check if product exists")
    void existsById_ShouldReturnCorrectBoolean() {
        // Given
        when(productRepository.existsById(testProductId)).thenReturn(true);

        // When
        boolean exists = productService.existsById(testProductId);

        // Then
        assertTrue(exists);
        verify(productRepository).existsById(testProductId);
    }

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct_WhenProductExists_ShouldUpdateProduct() {
        // Given
        Product updateData = new Product();
        updateData.setName("Updated Name");
        updateData.setPrice(new BigDecimal("199.99"));

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Optional<Product> result = productService.updateProduct(testProductId, updateData);

        // Then
        assertTrue(result.isPresent());
        verify(productEventService).createProductUpdatedEvent(testProduct);
        verify(productRepository).save(testProduct);
    }

    private Product createTestProduct() {
        Product product = new Product();
        product.setId(testProductId);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(100);
        product.setSku("TEST-SKU-001");
        product.setWeight(new BigDecimal("1.0"));
        product.setDimensions("10x10x10");
        product.setStatus(ProductStatus.ACTIVE);
        product.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }
}