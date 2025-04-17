package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return productService.findProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Product>> getProductsByStatus(@PathVariable ProductStatus status) {
        return ResponseEntity.ok(productService.findProductsByStatus(status));
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product savedProduct = productService.saveProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        if (!productService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Product> updateProductStatus(
            @PathVariable UUID id,
            @RequestParam ProductStatus status) {
        return productService.updateProductStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates a product with the provided data (PUT).
     * This method will replace the entire product.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProductFull(
            @PathVariable UUID id,
            @RequestBody Product product) {
        if (!productService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        product.setId(id); // Ensure the ID is set correctly
        Product updatedProduct = productService.saveProduct(product);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Partially updates a product with the provided data (PATCH).
     * This method only updates the fields that are provided in the request.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Product> updateProductPartial(
            @PathVariable UUID id,
            @RequestBody Product productData) {
        return productService.updateProduct(id, productData)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}