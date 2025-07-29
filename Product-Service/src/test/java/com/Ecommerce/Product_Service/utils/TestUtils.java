package com.Ecommerce.Product_Service.utils;

import com.Ecommerce.Product_Service.Entities.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

public class TestUtils {

    public static Product createTestProduct(String name, ProductStatus status) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setDescription("Test product description");
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

    public static Category createTestCategory(String name, UUID parentId, Integer level) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setParentId(parentId);
        category.setDescription("Test category description");
        category.setLevel(level);
        category.setCreatedAt(LocalDateTime.now());
        return category;
    }

    public static Inventory createTestInventory(Product product, Integer quantity) {
        Inventory inventory = new Inventory();
        inventory.setId(product.getId());
        inventory.setProduct(product);
        inventory.setQuantity(quantity);
        inventory.setLowStockThreshold(10);
        inventory.setWarehouseLocation("MAIN_WAREHOUSE");
        inventory.setLastRestocked(LocalDateTime.now());
        return inventory;
    }

    public static Review createTestReview(Product product, UUID userId, Integer rating) {
        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setProduct(product);
        review.setUserId(userId);
        review.setRating(rating);
        review.setComment("Test review comment");
        review.setVerified(false);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }

    public static Supplier createTestSupplier(String name) {
        Supplier supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setName(name);
        supplier.setContactInfo("test@supplier.com");
        supplier.setAddress("123 Test Street");
        supplier.setRating(new BigDecimal("4.5"));
        supplier.setCreatedAt(LocalDateTime.now());
        return supplier;
    }

    public static MockMultipartFile createTestImageFile(String filename, String contentType) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                "test image content".getBytes()
        );
    }

    public static String asJsonString(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
