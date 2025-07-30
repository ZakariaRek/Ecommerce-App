package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.*;
import com.Ecommerce.Product_Service.Payload.Product.*;
import com.Ecommerce.Product_Service.Services.CategoryService;
import com.Ecommerce.Product_Service.Services.FileStorageService;
import com.Ecommerce.Product_Service.Services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/products")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductMapper productMapper; // Add the mapper

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<Product> products = productService.findAllProducts();
        List<ProductResponseDTO> productDTOs = productMapper.toResponseDTOList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductResponseAllDto>> getAllProductsForFront() {
        List<Product> products = productService.findAllProducts();
        List<ProductResponseAllDto> productDTOs = productMapper.toResponseAllDTOList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/no-inventory")
    public ResponseEntity<List<ProductResponseDTO>> getProductsWithoutInventory() {
        List<Product> products = productService.getProductsWithoutInventory();
        List<ProductResponseDTO> productDTOs = productMapper.toResponseDTOList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable UUID id) {
        return productService.findProductById(id)
                .map(productMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByStatus(@PathVariable ProductStatus status) {
        List<Product> products = productService.findProductsByStatus(status);
        List<ProductResponseDTO> productDTOs = productMapper.toResponseDTOList(products);
        return ResponseEntity.ok(productDTOs);
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(@RequestBody ProductRequestDTO productDTO) {
        Product product = productMapper.toEntity(productDTO);
        Product savedProduct = productService.saveProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.toResponseDTO(savedProduct));
    }

    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createProductWithImages(
            @RequestPart("product") ProductRequestDTO productDTO,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        log.info("Creating product with images: {}", productDTO.getName());

        try {
            // Create product entity with categories
            Product product = productMapper.toEntity(productDTO);

            // Handle image uploads if provided
            List<String> imageUrls = new ArrayList<>();
            if (images != null && images.length > 0) {
                log.info("Processing {} images", images.length);

                for (MultipartFile image : images) {
                    if (!image.isEmpty()) {
                        validateImageFile(image);
                        String fileName = fileStorageService.storeFile(image);
                        String imageUrl = fileStorageService.getFileUrl(fileName);
                        imageUrls.add(imageUrl);
                        log.info("Uploaded image: {}", fileName);
                    }
                }

                product.setImages(imageUrls);
            }

            // Save product (this will also save the category relationships)
            Product savedProduct = productService.saveProduct(product);
            log.info("Product created successfully with ID: {} and {} categories",
                    savedProduct.getId(),
                    savedProduct.getCategories() != null ? savedProduct.getCategories().size() : 0);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("product", productMapper.toResponseDTO(savedProduct));
            response.put("imagesUploaded", imageUrls.size());
            response.put("imageUrls", imageUrls);
            response.put("categoriesAssigned", savedProduct.getCategories() != null ? savedProduct.getCategories().size() : 0);
            response.put("message", String.format("Product created successfully with %d images and %d categories",
                    imageUrls.size(),
                    savedProduct.getCategories() != null ? savedProduct.getCategories().size() : 0));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to create product with images", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to create product: " + e.getMessage()
            );
        }
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
                .map(productMapper::toResponseDTO)
                .map(ResponseEntity::ok)
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
                    productMapper.updateEntityFromDto(existingProduct, productDTO);
                    existingProduct.setId(id);
                    Product updatedProduct = productService.saveProduct(existingProduct);
                    return ResponseEntity.ok(productMapper.toResponseDTO(updatedProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}/with-images", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> updateProductWithImages(
            @PathVariable UUID id,
            @RequestPart("product") String productJson,
            @RequestParam(value = "images", required = false) MultipartFile[] newImages,
            @RequestParam(value = "replaceImages", defaultValue = "false") boolean replaceImages) {

        log.info("Updating product {} with images", id);

        try {
            // Check if product exists
            Optional<Product> existingProductOpt = productService.findProductById(id);
            if (existingProductOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }

            Product existingProduct = existingProductOpt.get();
            List<String> currentImages = new ArrayList<>(existingProduct.getImages());

            // Parse product updates
            ProductRequestDTO productDTO = objectMapper.readValue(productJson, ProductRequestDTO.class);
            productMapper.updateEntityFromDto(existingProduct, productDTO);

            // Handle image updates
            List<String> finalImageUrls = new ArrayList<>();

            if (!replaceImages) {
                // Keep existing images
                finalImageUrls.addAll(currentImages);
            } else {
                // Delete old images
                for (String imageUrl : currentImages) {
                    String fileName = fileStorageService.extractFileNameFromUrl(imageUrl);
                    if (fileName != null) {
                        fileStorageService.deleteFile(fileName);
                    }
                }
            }

            // Add new images
            if (newImages != null && newImages.length > 0) {
                for (MultipartFile image : newImages) {
                    if (!image.isEmpty()) {
                        validateImageFile(image);
                        String fileName = fileStorageService.storeFile(image);
                        String imageUrl = fileStorageService.getFileUrl(fileName);
                        finalImageUrls.add(imageUrl);
                    }
                }
            }

            existingProduct.setImages(finalImageUrls);

            // Save updated product
            Product updatedProduct = productService.saveProduct(existingProduct);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("product", productMapper.toResponseDTO(updatedProduct));
            response.put("totalImages", finalImageUrls.size());
            response.put("newImagesAdded", newImages != null ? newImages.length : 0);
            response.put("imagesReplaced", replaceImages);
            response.put("message", "Product updated successfully");

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update product with images", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Failed to update product: " + e.getMessage()
            );
        }
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
                    productMapper.updateEntityFromDto(existingProduct, productDTO);
                    Product updatedProduct = productService.saveProduct(existingProduct);
                    return ResponseEntity.ok(productMapper.toResponseDTO(updatedProduct));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Helper methods
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new RuntimeException("File size too large: " + file.getOriginalFilename());
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Invalid file type: " + contentType);
        }
    }
}