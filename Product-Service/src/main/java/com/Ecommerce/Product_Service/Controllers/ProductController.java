package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Payload.Request.ProductRequestDTO;
import com.Ecommerce.Product_Service.Payload.Response.ProductResponseDTO;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<Product> products = productService.findAllProducts();
        List<ProductResponseDTO> productDTOs = products.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable UUID id) {
        return productService.findProductById(id)
                .map(product -> ResponseEntity.ok(mapToDto(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByStatus(@PathVariable ProductStatus status) {
        List<Product> products = productService.findProductsByStatus(status);
        List<ProductResponseDTO> productDTOs = products.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs);
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@RequestBody ProductRequestDTO productDTO) {
        Product product = mapToEntity(productDTO);
        Product savedProduct = productService.saveProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapToDto(savedProduct));
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
    public ResponseEntity<ProductResponseDTO> updateProductStatus(
            @PathVariable UUID id,
            @RequestParam ProductStatus status) {
        return productService.updateProductStatus(id, status)
                .map(product -> ResponseEntity.ok(mapToDto(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates a product with the provided data (PUT).
     * This method will replace the entire product.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProductFull(
            @PathVariable UUID id,
            @RequestBody ProductRequestDTO productDTO) {
        if (!productService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return productService.findProductById(id)
                .map(existingProduct -> {
                    updateEntityFromDto(existingProduct, productDTO);
                    existingProduct.setId(id);
                    Product updatedProduct = productService.saveProduct(existingProduct);
                    return ResponseEntity.ok(mapToDto(updatedProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Partially updates a product with the provided data (PATCH).
     * This method only updates the fields that are provided in the request.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProductPartial(
            @PathVariable UUID id,
            @RequestBody ProductRequestDTO productDTO) {
        return productService.findProductById(id)
                .map(existingProduct -> {
                    updateEntityFromDto(existingProduct, productDTO);
                    Product updatedProduct = productService.saveProduct(existingProduct);
                    return ResponseEntity.ok(mapToDto(updatedProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Mapping methods
    private ProductResponseDTO mapToDto(Product product) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setSku(product.getSku());
        dto.setWeight(product.getWeight());
        dto.setDimensions(product.getDimensions());
        dto.setImages(product.getImages());
        dto.setStatus(product.getStatus());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }

    private Product mapToEntity(ProductRequestDTO dto) {
        Product product = new Product();
        updateEntityFromDto(product, dto);
        return product;
    }

    private void updateEntityFromDto(Product product, ProductRequestDTO dto) {
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getStock() != null) product.setStock(dto.getStock());
        if (dto.getSku() != null) product.setSku(dto.getSku());
        if (dto.getWeight() != null) product.setWeight(dto.getWeight());
        if (dto.getDimensions() != null) product.setDimensions(dto.getDimensions());
        if (dto.getImages() != null) product.setImages(dto.getImages());
        if (dto.getStatus() != null) product.setStatus(dto.getStatus());
    }
}