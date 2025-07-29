//package com.Ecommerce.Product_Service;
//
//import com.Ecommerce.Product_Service.Entities.Product;
//import com.Ecommerce.Product_Service.Entities.ProductStatus;
//import com.Ecommerce.Product_Service.Entities.Category;
//import com.Ecommerce.Product_Service.Entities.Inventory;
//import com.Ecommerce.Product_Service.Payload.Product.ProductRequestDTO;
//import com.Ecommerce.Product_Service.Repositories.ProductRepository;
//import com.Ecommerce.Product_Service.Repositories.CategoryRepository;
//import com.Ecommerce.Product_Service.Services.ProductService;
//import com.Ecommerce.Product_Service.Services.CategoryService;
//import com.Ecommerce.Product_Service.Services.InventoryService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.hamcrest.Matchers.*;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
//@TestPropertySource(properties = {
//        "spring.datasource.url=jdbc:h2:mem:testdb",
//        "spring.jpa.hibernate.ddl-auto=create-drop",
//        "spring.kafka.bootstrap-servers=localhost:9092",
//        "file.upload-dir=./test-uploads"
//})
//@ActiveProfiles("test")
//@Transactional
//@DisplayName("Product Service Integration Tests")
//class ProductServiceIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ProductService productService;
//
//    @Autowired
//    private CategoryService categoryService;
//
//    @Autowired
//    private InventoryService inventoryService;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private Category testCategory;
//    private Product testProduct;
//
//    @BeforeEach
//    void setUp() {
//        // Clean up database
//        productRepository.deleteAll();
//        categoryRepository.deleteAll();
//
//        // Create test category
//        testCategory = createTestCategory();
//        testCategory = categoryRepository.save(testCategory);
//
//        // Create test product
//        testProduct = createTestProduct();
//    }
//
//    @Test
//    @DisplayName("Should create product with complete flow")
//    void createProduct_CompleteFlow_ShouldWorkEndToEnd() throws Exception {
//        // Given
//        ProductRequestDTO productRequest = createProductRequestDTO();
//
//        // When - Create product via REST API
//        String response = mockMvc.perform(post("/products")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(productRequest)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.name", is("Integration Test Product")))
//                .andExpect(jsonPath("$.price", is(149.99)))
//                .andExpect(jsonPath("$.status", is("ACTIVE")))
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        // Parse response to get product ID
//        Product createdProduct = objectMapper.readValue(response, Product.class);
//        UUID productId = createdProduct.getId();
//
//        // Then - Verify product exists in database
//        Optional<Product> savedProduct = productRepository.findById(productId);
//        assertTrue(savedProduct.isPresent());
//        assertThat(savedProduct.get().getName()).isEqualTo("Integration Test Product");
//
//        // And - Verify product can be retrieved via API
//        mockMvc.perform(get("/products/{id}", productId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id", is(productId.toString())))
//                .andExpect(jsonPath("$.name", is("Integration Test Product")));
//    }
//
//    @Test
//    @DisplayName("Should create product with category association")
//    void createProduct_WithCategory_ShouldAssociateCategory() {
//        // Given
//        Product product = createTestProduct();
//        product.setCategories(Arrays.asList(testCategory));
//
//        // When
//        Product savedProduct = productService.saveProduct(product);
//
//        // Then
//        assertThat(savedProduct.getCategories()).hasSize(1);
//        assertThat(savedProduct.getCategories().get(0).getName()).isEqualTo("Test Category");
//
//        // Verify bidirectional relationship
//        Category retrievedCategory = categoryService.findCategoryById(testCategory.getId()).orElse(null);
//        assertNotNull(retrievedCategory);
//        assertThat(retrievedCategory.getProducts()).hasSize(1);
//        assertThat(retrievedCategory.getProducts().get(0).getId()).isEqualTo(savedProduct.getId());
//    }
//
//    @Test
//    @DisplayName("Should create inventory for product")
//    void createInventory_ForProduct_ShouldCreateSuccessfully() {
//        // Given
//        Product savedProduct = productService.saveProduct(testProduct);
//
//        // When
//        Inventory inventory = inventoryService.createInventoryForProduct(
//                savedProduct.getId(), 100, "MAIN_WAREHOUSE", 10);
//
//        // Then
//        assertThat(inventory).isNotNull();
//        assertThat(inventory.getQuantity()).isEqualTo(100);
//        assertThat(inventory.getWarehouseLocation()).isEqualTo("MAIN_WAREHOUSE");
//        assertThat(inventory.getLowStockThreshold()).isEqualTo(10);
//
//        // Verify product-inventory relationship
//        Optional<Product> productWithInventory = productService.findProductById(savedProduct.getId());
//        assertTrue(productWithInventory.isPresent());
//        assertThat(productWithInventory.get().getStock()).isEqualTo(100);
//    }
//
//    @Test
//    @DisplayName("Should update product status and reflect in inventory")
//    void updateProductStatus_ShouldUpdateStatusAndInventory() throws Exception {
//        // Given
//        Product savedProduct = productService.saveProduct(testProduct);
//        inventoryService.createInventoryForProduct(savedProduct.getId(), 50, "MAIN_WAREHOUSE", 5);
//
//        // When - Update product status to OUT_OF_STOCK
//        mockMvc.perform(patch("/products/{id}/status", savedProduct.getId())
//                        .param("status", "OUT_OF_STOCK"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status", is("OUT_OF_STOCK")));
//
//        // Then - Verify product status is updated
//        Optional<Product> updatedProduct = productService.findProductById(savedProduct.getId());
//        assertTrue(updatedProduct.isPresent());
//        assertThat(updatedProduct.get().getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
//    }
//
//    @Test
//    @DisplayName("Should handle product search and filtering")
//    void searchProducts_ByStatus_ShouldReturnFilteredResults() throws Exception {
//        // Given - Create products with different statuses
//        Product activeProduct = createTestProduct();
//        activeProduct.setName("Active Product");
//        activeProduct.setStatus(ProductStatus.ACTIVE);
//
//        Product inactiveProduct = createTestProduct();
//        inactiveProduct.setName("Inactive Product");
//        inactiveProduct.setStatus(ProductStatus.INACTIVE);
//
//        productService.saveProduct(activeProduct);
//        productService.saveProduct(inactiveProduct);
//
//        // When & Then - Search for active products
//        mockMvc.perform(get("/products/status/ACTIVE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(1)))
//                .andExpect(jsonPath("$[0].name", is("Active Product")))
//                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
//
//        // And - Search for inactive products
//        mockMvc.perform(get("/products/status/INACTIVE"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(1)))
//                .andExpect(jsonPath("$[0].name", is("Inactive Product")))
//                .andExpect(jsonPath("$[0].status", is("INACTIVE")));
//    }
//
//    @Test
//    @DisplayName("Should handle product batch operations")
//    void batchOperations_ShouldWorkCorrectly() {
//        // Given
//        Product product1 = createTestProduct();
//        product1.setName("Batch Product 1");
//        Product product2 = createTestProduct();
//        product2.setName("Batch Product 2");
//
//        Product savedProduct1 = productService.saveProduct(product1);
//        Product savedProduct2 = productService.saveProduct(product2);
//
//        List<UUID> productIds = Arrays.asList(savedProduct1.getId(), savedProduct2.getId());
//
//        // When
//        var batchResponse = productService.getBatchProductInfo(productIds);
//
//        // Then
//        assertThat(batchResponse).hasSize(2);
//        assertThat(batchResponse.get(0).getName()).isEqualTo("Batch Product 1");
//        assertThat(batchResponse.get(1).getName()).isEqualTo("Batch Product 2");
//    }
//
//    @Test
//    @DisplayName("Should handle inventory stock operations")
//    void inventoryOperations_ShouldWorkEndToEnd() {
//        // Given
//        Product savedProduct = productService.saveProduct(testProduct);
//        Inventory inventory = inventoryService.createInventoryForProduct(
//                savedProduct.getId(), 100, "MAIN_WAREHOUSE", 10);
//
//        // When - Update stock
//        Optional<Inventory> updatedInventory = inventoryService.updateStock(savedProduct.getId(), 150);
//
//        // Then
//        assertTrue(updatedInventory.isPresent());
//        assertThat(updatedInventory.get().getQuantity()).isEqualTo(150);
//
//        // When - Restock inventory
//        Optional<Inventory> restockedInventory = inventoryService.restockInventory(savedProduct.getId(), 50);
//
//        // Then
//        assertTrue(restockedInventory.isPresent());
//        assertThat(restockedInventory.get().getQuantity()).isEqualTo(200); // 150 + 50
//
//        // When - Reserve stock
//        boolean reserved = inventoryService.reserveStock(savedProduct.getId(), 30);
//
//        // Then
//        assertTrue(reserved);
//        Optional<Inventory> inventoryAfterReservation = inventoryService.findInventoryByProductId(savedProduct.getId());
//        assertTrue(inventoryAfterReservation.isPresent());
//        assertThat(inventoryAfterReservation.get().getQuantity()).isEqualTo(170); // 200 - 30
//    }
//
//    @Test
//    @DisplayName("Should handle product deletion with constraints")
//    void deleteProduct_WithInventory_ShouldHandleConstraints() throws Exception {
//        // Given
//        Product savedProduct = productService.saveProduct(testProduct);
//        inventoryService.createInventoryForProduct(savedProduct.getId(), 0, "MAIN_WAREHOUSE", 10);
//
//        // When - Delete product
//        mockMvc.perform(delete("/products/{id}", savedProduct.getId()))
//                .andExpect(status().isNoContent());
//
//        // Then - Verify product is deleted
//        Optional<Product> deletedProduct = productService.findProductById(savedProduct.getId());
//        assertTrue(deletedProduct.isEmpty());
//    }
//
//    @Test
//    @DisplayName("Should handle category hierarchy operations")
//    void categoryHierarchy_ShouldWorkCorrectly() {
//        // Given
//        Category parentCategory = createTestCategory();
//        parentCategory.setName("Parent Category");
//        parentCategory.setLevel(0);
//        Category savedParent = categoryService.addCategory(parentCategory);
//
//        Category childCategory = createTestCategory();
//        childCategory.setName("Child Category");
//        childCategory.setParentId(savedParent.getId());
//        childCategory.setLevel(1);
//
//        // When
//        Category savedChild = categoryService.addCategory(childCategory);
//
//        // Then
//        assertThat(savedChild.getParentId()).isEqualTo(savedParent.getId());
//        assertThat(savedChild.getLevel()).isEqualTo(1);
//
//        // Verify hierarchy queries
//        List<Category> rootCategories = categoryService.findRootCategories();
//        assertThat(rootCategories).hasSize(2); // testCategory + parentCategory
//
//        List<Category> subcategories = categoryService.findSubcategories(savedParent.getId());
//        assertThat(subcategories).hasSize(1);
//        assertThat(subcategories.get(0).getName()).isEqualTo("Child Category");
//    }
//
//    @Test
//    @DisplayName("Should validate business rules")
//    void businessRules_ShouldBeEnforced() {
//        // Test 1: Cannot create inventory for non-existent product
//        UUID nonExistentProductId = UUID.randomUUID();
//        assertThrows(IllegalArgumentException.class, () -> {
//            inventoryService.createInventoryForProduct(nonExistentProductId, 100, "MAIN_WAREHOUSE", 10);
//        });
//
//        // Test 2: Cannot create duplicate inventory
//        Product savedProduct = productService.saveProduct(testProduct);
//        inventoryService.createInventoryForProduct(savedProduct.getId(), 100, "MAIN_WAREHOUSE", 10);
//
//        assertThrows(IllegalStateException.class, () -> {
//            inventoryService.createInventoryForProduct(savedProduct.getId(), 50, "WAREHOUSE_B", 5);
//        });
//
//        // Test 3: Cannot reserve more stock than available
//        boolean reserved = inventoryService.reserveStock(savedProduct.getId(), 150); // Only 100 available
//        assertFalse(reserved);
//    }
//
//    private Product createTestProduct() {
//        Product product = new Product();
//        product.setName("Integration Test Product");
//        product.setDescription("Test product for integration testing");
//        product.setPrice(new BigDecimal("149.99"));
//        product.setStock(50);
//        product.setSku("INT-TEST-001");
//        product.setWeight(new BigDecimal("2.5"));
//        product.setDimensions("20x15x10 cm");
//        product.setStatus(ProductStatus.ACTIVE);
//        product.setImages(Arrays.asList("test1.jpg", "test2.jpg"));
//        return product;
//    }
//
//    private Category createTestCategory() {
//        Category category = new Category();
//        category.setName("Test Category");
//        category.setDescription("Test category for integration testing");
//        category.setLevel(0);
//        return category;
//    }
//
//    private ProductRequestDTO createProductRequestDTO() {
//        ProductRequestDTO dto = new ProductRequestDTO();
//        dto.setName("Integration Test Product");
//        dto.setDescription("Test product for integration testing");
//        dto.setPrice(new BigDecimal("149.99"));
//        dto.setStock(50);
//        dto.setSku("INT-TEST-001");
//        dto.setWeight(new BigDecimal("2.5"));
//        dto.setDimensions("20x15x10 cm");
//        dto.setStatus(ProductStatus.ACTIVE);
//        dto.setImages(Arrays.asList("test1.jpg", "test2.jpg"));
//        dto.setCategoryIds(Arrays.asList(testCategory.getId()));
//        return dto;
//    }
//}