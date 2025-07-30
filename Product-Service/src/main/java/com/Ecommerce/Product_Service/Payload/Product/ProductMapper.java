package com.Ecommerce.Product_Service.Payload.Product;

import com.Ecommerce.Product_Service.Entities.*;
import com.Ecommerce.Product_Service.Payload.Categorie.CategoryResponseDtoForPro;
import com.Ecommerce.Product_Service.Payload.Discont.DiscountResponseDtoForPro;
import com.Ecommerce.Product_Service.Payload.Review.ReviewResponseDtoFroPro;
import com.Ecommerce.Product_Service.Services.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductMapper {

    @Autowired
    @Lazy // Prevent circular dependency
    private CategoryService categoryService;

    /**
     * Convert ProductRequestDTO to Product entity
     */
    public Product toEntity(ProductRequestDTO dto) {
        if (dto == null) return null;

        Product product = new Product();
        updateEntityFromDto(product, dto);
        return product;
    }

    /**
     * Convert Product entity to ProductResponseDTO
     */
    public ProductResponseDTO toResponseDTO(Product entity) {
        if (entity == null) return null;

        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setStock(entity.getStock());
        dto.setSku(entity.getSku());
        dto.setWeight(entity.getWeight());
        dto.setDimensions(entity.getDimensions());
        dto.setImages(entity.getImages());
        dto.setStatus(entity.getStatus());
        dto.setReviews(entity.getReviews());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    /**
     * Convert Product entity to ProductResponseAllDto (with full relations)
     */
    public ProductResponseAllDto toResponseAllDTO(Product entity) {
        if (entity == null) return null;

        ProductResponseAllDto dto = new ProductResponseAllDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setStock(entity.getStock());
        dto.setSku(entity.getSku());
        dto.setWeight(entity.getWeight());
        dto.setDimensions(entity.getDimensions());
        dto.setImages(entity.getImages());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Map categories
        if (entity.getCategories() != null) {
            dto.setCategories(entity.getCategories().stream()
                    .map(this::toCategoryResponseDto)
                    .collect(Collectors.toList()));
        }

        // Map discounts
        if (entity.getDiscounts() != null) {
            dto.setDiscounts(entity.getDiscounts().stream()
                    .map(this::toDiscountResponseDto)
                    .collect(Collectors.toList()));
        }

        // Map reviews
        if (entity.getReviews() != null) {
            dto.setReviews(entity.getReviews().stream()
                    .map(this::toReviewResponseDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Convert Product entity to ProductSummaryDTO
     */
    public ProductSummaryDTO toSummaryDTO(Product entity) {
        if (entity == null) return null;

        ProductSummaryDTO dto = new ProductSummaryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSku(entity.getSku());
        dto.setPrice(entity.getPrice());
        dto.setStockQuantity(entity.getStock());
        dto.setIsActive(entity.getStatus() == ProductStatus.ACTIVE);

        // Get primary category name
        if (entity.getCategories() != null && !entity.getCategories().isEmpty()) {
            dto.setCategory(entity.getCategories().get(0).getName());
        }

        // Get supplier names
        if (entity.getSuppliers() != null && !entity.getSuppliers().isEmpty()) {
            List<String> supplierNames = entity.getSuppliers().stream()
                    .map(Supplier::getName)
                    .collect(Collectors.toList());
            dto.setSupplierNames(supplierNames);
        }

        return dto;
    }

    /**
     * Convert Product entity to ProductBatchResponseDTO
     */
    public ProductBatchResponseDTO toBatchResponseDTO(Product entity) {
        if (entity == null) return null;

        // Calculate discount information
        BigDecimal totalDiscountValue = BigDecimal.ZERO;
        String discountTypes = "";

        if (entity.getDiscounts() != null) {
            totalDiscountValue = entity.getDiscounts().stream()
                    .filter(discount -> discount.isActive())
                    .map(discount -> discount.applyDiscount(entity.getPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            discountTypes = entity.getDiscounts().stream()
                    .filter(discount -> discount.isActive())
                    .map(discount -> discount.getDiscountType().toString())
                    .collect(Collectors.joining(", "));
        }

        return ProductBatchResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .imagePath(getFirstImagePath(entity))
                .inStock(isProductInStock(entity))
                .availableQuantity(getAvailableQuantity(entity))
                .status(entity.getStatus())
                .discountValue(totalDiscountValue)
                .discountType(discountTypes.isEmpty() ? null : discountTypes)
                .build();
    }

    /**
     * Update existing Product entity from ProductRequestDTO
     */
    public void updateEntityFromDto(Product product, ProductRequestDTO dto) {
        if (product == null || dto == null) return;

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

        // Handle Category Assignment
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            try {
                log.info("Assigning {} categories to product", dto.getCategoryIds().size());

                List<Category> categories = categoryService.findByIds(dto.getCategoryIds());

                // Validate that all requested categories were found
                if (categories.size() != dto.getCategoryIds().size()) {
                    List<UUID> foundIds = categories.stream()
                            .map(Category::getId)
                            .collect(Collectors.toList());

                    List<UUID> missingIds = dto.getCategoryIds().stream()
                            .filter(id -> !foundIds.contains(id))
                            .collect(Collectors.toList());

                    log.warn("Some categories not found: {}", missingIds);
                }

                product.setCategories(categories);
                log.info("Successfully assigned {} categories to product", categories.size());

            } catch (Exception e) {
                log.error("Error assigning categories to product", e);
                throw new RuntimeException("Failed to assign categories: " + e.getMessage());
            }
        }

        // Handle Supplier Assignment (if you have suppliers)
        if (dto.getSupplierIds() != null && !dto.getSupplierIds().isEmpty()) {
            try {
                log.info("Assigning {} suppliers to product", dto.getSupplierIds().size());
                // Note: You'll need to implement supplier assignment based on your SupplierService
                log.info("Supplier assignment would go here - implement based on your Supplier entity");
            } catch (Exception e) {
                log.error("Error assigning suppliers to product", e);
                throw new RuntimeException("Failed to assign suppliers: " + e.getMessage());
            }
        }
    }

    /**
     * Convert list of Product entities to list of ProductResponseDTOs
     */
    public List<ProductResponseDTO> toResponseDTOList(List<Product> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Product entities to list of ProductResponseAllDtos
     */
    public List<ProductResponseAllDto> toResponseAllDTOList(List<Product> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(this::toResponseAllDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Product entities to list of ProductSummaryDTOs
     */
    public List<ProductSummaryDTO> toSummaryDTOList(List<Product> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Product entities to list of ProductBatchResponseDTOs
     */
    public List<ProductBatchResponseDTO> toBatchResponseDTOList(List<Product> entities) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(this::toBatchResponseDTO)
                .collect(Collectors.toList());
    }

    // ====== PRIVATE HELPER METHODS ======

    private CategoryResponseDtoForPro toCategoryResponseDto(Category category) {
        if (category == null) return null;

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
        if (discount == null) return null;

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
        if (review == null) return null;

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

    private String getFirstImagePath(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0);
        }
        return "/api/products/images/default-product.png"; // Default image path
    }

    private Boolean isProductInStock(Product product) {
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK ||
                product.getStatus() == ProductStatus.DISCONTINUED) {
            return false;
        }

        if (product.getInventory() != null) {
            return product.getInventory().getQuantity() != null &&
                    product.getInventory().getQuantity() > 0;
        }

        return product.getStock() != null && product.getStock() > 0;
    }

    private Integer getAvailableQuantity(Product product) {
        if (product.getInventory() != null && product.getInventory().getQuantity() != null) {
            return product.getInventory().getQuantity();
        }
        return product.getStock() != null ? product.getStock() : 0;
    }
}