package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Services.CategoryService;
import com.Ecommerce.Product_Service.Services.FileStorageService;
import com.Ecommerce.Product_Service.Services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("Product Controller Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;
    private UUID testProductId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testProduct = createTestProduct();
    }

    @Test
    @DisplayName("GET /products - Should return all products")
    void getAllProducts_ShouldReturnProductList() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productService.findAllProducts()).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testProduct.getId().toString())))
                .andExpect(jsonPath("$[0].name", is(testProduct.getName())))
                .andExpect(jsonPath("$[0].price", is(testProduct.getPrice().doubleValue())));

        verify(productService).findAllProducts();
    }

    @Test
    @DisplayName("GET /products/{id} - Should return product when found")
    void getProductById_WhenProductExists_ShouldReturnProduct() throws Exception {
        // Given
        when(productService.findProductById(testProductId)).thenReturn(Optional.of(testProduct));

        // When & Then
        mockMvc.perform(get("/products/{id}", testProductId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testProduct.getId().toString())))
                .andExpect(jsonPath("$.name", is(testProduct.getName())))
                .andExpect(jsonPath("$.status", is(testProduct.getStatus().toString())));

        verify(productService).findProductById(testProductId);
    }

    @Test
    @DisplayName("GET /products/{id} - Should return 404 when product not found")
    void getProductById_WhenProductNotExists_ShouldReturn404() throws Exception {
        // Given
        when(productService.findProductById(testProductId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/products/{id}", testProductId))
                .andExpect(status().isNotFound());

        verify(productService).findProductById(testProductId);
    }

    @Test
    @DisplayName("POST /products - Should create product successfully")
    void createProduct_WithValidData_ShouldCreateProduct() throws Exception {
        // Given
        Product newProduct = createTestProduct();
        newProduct.setId(null); // New product shouldn't have ID

        when(productService.saveProduct(any(Product.class))).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(testProduct.getId().toString())))
                .andExpect(jsonPath("$.name", is(testProduct.getName())));

        verify(productService).saveProduct(any(Product.class));
    }

    @Test
    @DisplayName("POST /products/with-images - Should create product with images")
    void createProductWithImages_ShouldCreateProductAndUploadImages() throws Exception {
        // Given
        MockMultipartFile image1 = new MockMultipartFile(
                "images", "test1.jpg", "image/jpeg", "image1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile(
                "images", "test2.jpg", "image/jpeg", "image2".getBytes());
        MockMultipartFile productData = new MockMultipartFile(
                "product", "", "application/json", objectMapper.writeValueAsBytes(testProduct));

        when(fileStorageService.storeFile(any())).thenReturn("stored-image.jpg");
        when(fileStorageService.getFileUrl(any())).thenReturn("/api/products/images/stored-image.jpg");
        when(productService.saveProduct(any(Product.class))).thenReturn(testProduct);

        // When & Then
        mockMvc.perform(multipart("/products/with-images")
                        .file(image1)
                        .file(image2)
                        .file(productData)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.product.id", is(testProduct.getId().toString())))
                .andExpect(jsonPath("$.imagesUploaded", is(2)));

        verify(fileStorageService, times(2)).storeFile(any());
        verify(productService).saveProduct(any(Product.class));
    }

    @Test
    @DisplayName("PUT /products/{id} - Should update product")
    void updateProduct_WithValidData_ShouldUpdateProduct() throws Exception {
        // Given
        Product updatedProduct = createTestProduct();
        updatedProduct.setName("Updated Product Name");

        when(productService.existsById(testProductId)).thenReturn(true);
        when(productService.findProductById(testProductId)).thenReturn(Optional.of(testProduct));
        when(productService.saveProduct(any(Product.class))).thenReturn(updatedProduct);

        // When & Then
        mockMvc.perform(put("/products/{id}", testProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Updated Product Name")));

        verify(productService).saveProduct(any(Product.class));
    }

    @Test
    @DisplayName("DELETE /products/{id} - Should delete product")
    void deleteProduct_WhenProductExists_ShouldDeleteProduct() throws Exception {
        // Given
        when(productService.existsById(testProductId)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/products/{id}", testProductId))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(testProductId);
    }

    @Test
    @DisplayName("DELETE /products/{id} - Should return 404 when product not found")
    void deleteProduct_WhenProductNotExists_ShouldReturn404() throws Exception {
        // Given
        when(productService.existsById(testProductId)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/products/{id}", testProductId))
                .andExpect(status().isNotFound());

        verify(productService, never()).deleteProduct(testProductId);
    }

    @Test
    @DisplayName("PATCH /products/{id}/status - Should update product status")
    void updateProductStatus_WithValidStatus_ShouldUpdateStatus() throws Exception {
        // Given
        ProductStatus newStatus = ProductStatus.INACTIVE;
        Product updatedProduct = createTestProduct();
        updatedProduct.setStatus(newStatus);

        when(productService.updateProductStatus(testProductId, newStatus))
                .thenReturn(Optional.of(updatedProduct));

        // When & Then
        mockMvc.perform(patch("/products/{id}/status", testProductId)
                        .param("status", newStatus.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(newStatus.toString())));

        verify(productService).updateProductStatus(testProductId, newStatus);
    }

    @Test
    @DisplayName("GET /products/status/{status} - Should return products by status")
    void getProductsByStatus_ShouldReturnFilteredProducts() throws Exception {
        // Given
        ProductStatus status = ProductStatus.ACTIVE;
        List<Product> products = Arrays.asList(testProduct);
        when(productService.findProductsByStatus(status)).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/products/status/{status}", status))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is(status.toString())));

        verify(productService).findProductsByStatus(status);
    }

    @Test
    @DisplayName("GET /products/no-inventory - Should return products without inventory")
    void getProductsWithoutInventory_ShouldReturnProducts() throws Exception {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productService.getProductsWithoutInventory()).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/products/no-inventory"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));

        verify(productService).getProductsWithoutInventory();
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