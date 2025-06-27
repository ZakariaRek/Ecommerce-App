package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Services.FileStorageService;
import com.Ecommerce.Product_Service.Services.ProductService;
import com.Ecommerce.Product_Service.Entities.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/images")
@Slf4j
public class ImageController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ProductService productService;

    /**
     * Upload single image for a product
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileStorageService.storeFile(file);
            String fileUrl = fileStorageService.getFileUrl(fileName);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("fileUrl", fileUrl);
            response.put("message", "Image uploaded successfully");
            response.put("fileSize", file.getSize());

            log.info("Image uploaded successfully: {}", fileName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload image", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Upload multiple images for a product
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files) {
        try {
            if (files.length == 0) {
                throw new RuntimeException("No files provided");
            }

            if (files.length > 10) { // Limit to 10 images per upload
                throw new RuntimeException("Maximum 10 images allowed per upload");
            }

            List<String> fileNames = fileStorageService.storeFiles(files);
            List<String> fileUrls = fileStorageService.getFileUrls(fileNames);

            Map<String, Object> response = new HashMap<>();
            response.put("fileNames", fileNames);
            response.put("fileUrls", fileUrls);
            response.put("message", "Images uploaded successfully");
            response.put("totalFiles", files.length);

            log.info("{} images uploaded successfully", files.length);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload multiple images", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Upload and associate images with a specific product
     */
    @PostMapping("/upload/product/{productId}")
    public ResponseEntity<Map<String, Object>> uploadProductImages(
            @PathVariable UUID productId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "replace", defaultValue = "false") boolean replace) {
        try {
            // Check if product exists
            Optional<Product> productOpt = productService.findProductById(productId);
            if (productOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }

            Product product = productOpt.get();
            List<String> existingImages = new ArrayList<>(product.getImages());

            // Upload new images
            List<String> newFileNames = fileStorageService.storeFiles(files);
            List<String> newFileUrls = fileStorageService.getFileUrls(newFileNames);

            if (replace) {
                // Delete old images if replacing
                for (String existingUrl : existingImages) {
                    String fileName = fileStorageService.extractFileNameFromUrl(existingUrl);
                    if (fileName != null) {
                        fileStorageService.deleteFile(fileName);
                    }
                }
                product.setImages(newFileUrls);
            } else {
                // Add to existing images
                product.getImages().addAll(newFileUrls);
            }

            // Save updated product
            productService.saveProduct(product);

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("newFileNames", newFileNames);
            response.put("newFileUrls", newFileUrls);
            response.put("allImages", product.getImages());
            response.put("message", replace ? "Product images replaced successfully" : "Images added to product successfully");

            log.info("Images uploaded and associated with product {}: {}", productId, newFileNames);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload product images for product {}", productId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Serve/download images
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        try {
            Resource resource = fileStorageService.loadFileAsResource(fileName);

            // Determine file's content type
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                log.info("Could not determine file type for {}", fileName);
            }

            // Fallback to default content type
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to download file {}", fileName, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + fileName);
        }
    }

    /**
     * Delete an image
     */
    @DeleteMapping("/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable String fileName) {
        try {
            boolean deleted = fileStorageService.deleteFile(fileName);

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("deleted", deleted);
            response.put("message", deleted ? "Image deleted successfully" : "Image not found");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete image {}", fileName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete image");
        }
    }

    /**
     * Remove image from a specific product
     */
    @DeleteMapping("/product/{productId}/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> removeImageFromProduct(
            @PathVariable UUID productId,
            @PathVariable String fileName) {
        try {
            Optional<Product> productOpt = productService.findProductById(productId);
            if (productOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }

            Product product = productOpt.get();
            String fileUrl = fileStorageService.getFileUrl(fileName);

            boolean removed = product.getImages().removeIf(url -> url.equals(fileUrl));

            if (removed) {
                productService.saveProduct(product);
                fileStorageService.deleteFile(fileName);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("fileName", fileName);
            response.put("removed", removed);
            response.put("remainingImages", product.getImages());
            response.put("message", removed ? "Image removed from product successfully" : "Image not found in product");

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to remove image {} from product {}", fileName, productId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove image from product");
        }
    }

    /**
     * Get all images for a product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProductImages(@PathVariable UUID productId) {
        try {
            Optional<Product> productOpt = productService.findProductById(productId);
            if (productOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }

            Product product = productOpt.get();
            List<String> images = product.getImages();

            // Verify which images actually exist on filesystem
            List<Map<String, Object>> imageDetails = new ArrayList<>();
            for (String imageUrl : images) {
                String fileName = fileStorageService.extractFileNameFromUrl(imageUrl);
                Map<String, Object> detail = new HashMap<>();
                detail.put("url", imageUrl);
                detail.put("fileName", fileName);
                detail.put("exists", fileName != null && fileStorageService.fileExists(fileName));
                detail.put("size", fileName != null ? fileStorageService.getFileSize(fileName) : 0);
                imageDetails.add(detail);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("totalImages", images.size());
            response.put("images", imageDetails);

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get images for product {}", productId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get product images");
        }
    }

    /**
     * Health check endpoint for image service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Image Storage Service");
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }
}