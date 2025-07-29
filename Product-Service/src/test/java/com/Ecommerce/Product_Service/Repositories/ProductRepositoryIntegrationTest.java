package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Product Repository Integration Tests")
class ProductRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product activeProduct;
    private Product inactiveProduct;
    private Product outOfStockProduct;

    @BeforeEach
    void setUp() {
        // Create test products with different statuses
        activeProduct = createTestProduct("Active Product", ProductStatus.ACTIVE);
        inactiveProduct = createTestProduct("Inactive Product", ProductStatus.INACTIVE);
        outOfStockProduct = createTestProduct("Out of Stock Product", ProductStatus.OUT_OF_STOCK);

        // Persist products
        entityManager.persistAndFlush(activeProduct);
        entityManager.persistAndFlush(inactiveProduct);
        entityManager.persistAndFlush(outOfStockProduct);
    }

    @Test
    @DisplayName("Should find all products")
    void findAll_ShouldReturnAllProducts() {
        // When
        List<Product> products = productRepository.findAll();

        // Then
        assertThat(products).hasSize(3);
        assertThat(products).extracting(Product::getName)
                .containsExactlyInAnyOrder("Active Product", "Inactive Product", "Out of Stock Product");
    }

    @Test
    @DisplayName("Should find product by ID")
    void findById_WhenProductExists_ShouldReturnProduct() {
        // When
        Optional<Product> found = productRepository.findById(activeProduct.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Active Product");
        assertThat(found.get().getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return empty when product not found")
    void findById_WhenProductNotExists_ShouldReturnEmpty() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<Product> found = productRepository.findById(nonExistentId);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find products by status")
    void findByStatus_ShouldReturnProductsWithSpecificStatus() {
        // When
        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE);
        List<Product> inactiveProducts = productRepository.findByStatus(ProductStatus.INACTIVE);

        // Then
        assertThat(activeProducts).hasSize(1);
        assertThat(activeProducts.get(0).getName()).isEqualTo("Active Product");

        assertThat(inactiveProducts).hasSize(1);
        assertThat(inactiveProducts.get(0).getName()).isEqualTo("Inactive Product");
    }

    @Test
    @DisplayName("Should find products without inventory")
    void findByInventoryIsNull_ShouldReturnProductsWithoutInventory() {
        // When
        List<Product> productsWithoutInventory = productRepository.findByInventoryIsNull();

        // Then
        assertThat(productsWithoutInventory).hasSize(3); // All test products have null inventory
        assertThat(productsWithoutInventory).extracting(Product::getName)
                .containsExactlyInAnyOrder("Active Product", "Inactive Product", "Out of Stock Product");
    }

    @Test
    @DisplayName("Should save new product")
    void save_NewProduct_ShouldPersistProduct() {
        // Given
        Product newProduct = createTestProduct("New Product", ProductStatus.ACTIVE);

        // When
        Product saved = productRepository.save(newProduct);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("New Product");

        // Verify it's persisted
        Optional<Product> found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("New Product");
    }

    @Test
    @DisplayName("Should update existing product")
    void save_ExistingProduct_ShouldUpdateProduct() {
        // Given
        activeProduct.setName("Updated Product Name");
        activeProduct.setPrice(new BigDecimal("199.99"));

        // When
        Product updated = productRepository.save(activeProduct);

        // Then
        assertThat(updated.getName()).isEqualTo("Updated Product Name");
        assertThat(updated.getPrice()).isEqualTo(new BigDecimal("199.99"));

        // Verify it's updated in database
        entityManager.flush();
        entityManager.clear();

        Optional<Product> found = productRepository.findById(activeProduct.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Updated Product Name");
        assertThat(found.get().getPrice()).isEqualTo(new BigDecimal("199.99"));
    }

    @Test
    @DisplayName("Should delete product")
    void deleteById_ShouldRemoveProduct() {
        // Given
        UUID productId = activeProduct.getId();

        // When
        productRepository.deleteById(productId);

        // Then
        Optional<Product> found = productRepository.findById(productId);
        assertThat(found).isEmpty();

        // Verify other products still exist
        assertThat(productRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Should check if product exists")
    void existsById_ShouldReturnCorrectResult() {
        // When & Then
        assertThat(productRepository.existsById(activeProduct.getId())).isTrue();
        assertThat(productRepository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("Should count all products")
    void count_ShouldReturnCorrectCount() {
        // When
        long count = productRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find products by multiple IDs")
    void findAllById_ShouldReturnMatchingProducts() {
        // Given
        List<UUID> ids = Arrays.asList(activeProduct.getId(), inactiveProduct.getId());

        // When
        List<Product> products = productRepository.findAllById(ids);

        // Then
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName)
                .containsExactlyInAnyOrder("Active Product", "Inactive Product");
    }

    @Test
    @DisplayName("Should delete all products")
    void deleteAll_ShouldRemoveAllProducts() {
        // When
        productRepository.deleteAll();

        // Then
        assertThat(productRepository.count()).isEqualTo(0);
        assertThat(productRepository.findAll()).isEmpty();
    }

    private Product createTestProduct(String name, ProductStatus status) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Test description for " + name);
        product.setPrice(new BigDecimal("99.99"));
        product.setStock(100);
        product.setSku("TEST-" + name.replaceAll(" ", "-").toUpperCase());
        product.setWeight(new BigDecimal("1.0"));
        product.setDimensions("10x10x10 cm");
        product.setStatus(status);
        product.setImages(Arrays.asList("image1.jpg", "image2.jpg"));
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }
}