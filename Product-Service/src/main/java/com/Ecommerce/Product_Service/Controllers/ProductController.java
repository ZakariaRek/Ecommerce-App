package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.*;
import com.Ecommerce.Product_Service.Payload.Categorie.CategoryResponseDtoForPro;
import com.Ecommerce.Product_Service.Payload.Discont.DiscountResponseDtoForPro;
import com.Ecommerce.Product_Service.Payload.Product.ProductRequestDTO;
import com.Ecommerce.Product_Service.Payload.Product.ProductResponseAllDto;
import com.Ecommerce.Product_Service.Payload.Product.ProductResponseDTO;
import com.Ecommerce.Product_Service.Payload.Review.ReviewResponseDtoFroPro;
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
import java.util.stream.Collectors;

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
    private  CategoryService categoryService; // Add this


    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<Product> products = productService.findAllProducts();
        List<ProductResponseDTO> productDTOs = products.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs);
    }
    @GetMapping("/all")
    public ResponseEntity<List<ProductResponseAllDto>> getAllProductsForFront() {
        List<Product> products = productService.findAllProducts();
        List<ProductResponseAllDto> productDTOs = products.stream()
                .map(this::mapToDto1)
                .collect(Collectors.toList());
        return ResponseEntity.ok(productDTOs);
    }
    @GetMapping("/no-inventory")
    public ResponseEntity<List<ProductResponseDTO>> getProductsWithoutInventory() {
        List<Product> products = productService.getProductsWithoutInventory();
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
    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createProductWithImages(
            @RequestPart("product") ProductRequestDTO productDTO,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        log.info("Creating product with images: {}", productDTO.getName());

        try {
            // ✅ Create product entity with categories
            Product product = mapToEntity(productDTO);

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
            response.put("product", mapToDto(savedProduct));
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
            updateEntityFromDto(existingProduct, productDTO);

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
            response.put("product", mapToDto(updatedProduct));
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
                    updateEntityFromDto(existingProduct, productDTO);
                    Product updatedProduct = productService.saveProduct(existingProduct);
                    return ResponseEntity.ok(mapToDto(updatedProduct));
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
    private ProductResponseAllDto mapToDto1(Product product) {
        ProductResponseAllDto dto = new ProductResponseAllDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setSku(product.getSku());
        dto.setWeight(product.getWeight());
        dto.setDimensions(product.getDimensions());
        dto.setImages(product.getImages());
        if (product.getCategories() != null) {
            dto.setCategories(product.getCategories().stream()
                    .map(this::toCategoryResponseDto)
                    .collect(Collectors.toList()));
        }

        // Map discounts without products
        if (product.getDiscounts() != null) {
            dto.setDiscounts(product.getDiscounts().stream()
                    .map(this::toDiscountResponseDto)
                    .collect(Collectors.toList()));
        }

        // Map reviews without products
        if (product.getReviews() != null) {
            dto.setReviews(product.getReviews().stream()
                    .map(this::toReviewResponseDto)
                    .collect(Collectors.toList()));
        }
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
        if (dto.getStatus() != null) product.setStatus(dto.getStatus());
        if (dto.getImages() != null) {
            product.setImages(dto.getImages().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        // ✅ Handle Category Assignment - FIXED VERSION
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            try {
                log.info("Assigning {} categories to product", dto.getCategoryIds().size());

                // Use the correct method - either categoryService.findByIds or categoryRepository.findAllById
                List<Category> categories = categoryService.findByIds(dto.getCategoryIds());
                // OR directly use: categoryRepository.findAllById(dto.getCategoryIds());

                // Validate that all requested categories were found
                if (categories.size() != dto.getCategoryIds().size()) {
                    Set<UUID> foundIds = categories.stream()
                            .map(Category::getId)
                            .collect(Collectors.toSet());

                    List<UUID> missingIds = dto.getCategoryIds().stream()
                            .filter(id -> !foundIds.contains(id))
                            .collect(Collectors.toList());

                    log.warn("Some categories not found: {}", missingIds);
                    // You can either throw an exception or continue with found categories
                    // throw new RuntimeException("Categories not found: " + missingIds);
                }

                product.setCategories(categories);
                log.info("Successfully assigned {} categories to product", categories.size());

            } catch (Exception e) {
                log.error("Error assigning categories to product", e);
                throw new RuntimeException("Failed to assign categories: " + e.getMessage());
            }
        }

        // ✅ Handle Supplier Assignment (if you have suppliers)
        if (dto.getSupplierIds() != null && !dto.getSupplierIds().isEmpty()) {
            try {
                log.info("Assigning {} suppliers to product", dto.getSupplierIds().size());

                // Assuming you have a SupplierService - adjust as needed
                // List<Supplier> suppliers = supplierService.findAllById(dto.getSupplierIds());
                // product.setSuppliers(suppliers);

                log.info("Supplier assignment would go here - implement based on your Supplier entity");

            } catch (Exception e) {
                log.error("Error assigning suppliers to product", e);
                throw new RuntimeException("Failed to assign suppliers: " + e.getMessage());
            }
        }
    }

    // 3. You might need to add this method to your CategoryService
    // If it doesn't exist, add it to CategoryService interface and implementation

    private CategoryResponseDtoForPro toCategoryResponseDto(Category category) {
        CategoryResponseDtoForPro dto = new CategoryResponseDtoForPro();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        dto.setDescription(category.getDescription());
        dto.setImageUrl(category.getImageUrl());
        dto.setLevel(category.getLevel());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }

    private DiscountResponseDtoForPro toDiscountResponseDto(Discount discount) {
        DiscountResponseDtoForPro dto = new DiscountResponseDtoForPro();
        dto.setId(discount.getId());
        dto.setDiscountType(discount.getDiscountType());
        dto.setDiscountValue(discount.getDiscountValue());
        dto.setStartDate(discount.getStartDate());
        dto.setEndDate(discount.getEndDate());
        dto.setMinPurchaseAmount(discount.getMinPurchaseAmount());
        dto.setMaxDiscountAmount(discount.getMaxDiscountAmount());
        return dto;
    }

    private ReviewResponseDtoFroPro toReviewResponseDto(Review review) {
        ReviewResponseDtoFroPro dto = new ReviewResponseDtoFroPro();
        dto.setId(review.getId());
        dto.setUserId(review.getUserId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setVerified(review.getVerified());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
    // Mapping methods




}