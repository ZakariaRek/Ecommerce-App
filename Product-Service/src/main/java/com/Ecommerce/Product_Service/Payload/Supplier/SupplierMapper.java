// SupplierMapper.java
package com.Ecommerce.Product_Service.Payload.Supplier;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Entities.Supplier;

import com.Ecommerce.Product_Service.Payload.Product.ProductSummaryDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SupplierMapper {

    /**
     * Convert SupplierRequestDTO to Supplier entity
     */
    public Supplier toEntity(SupplierRequestDTO dto) {
        if (dto == null) return null;

        Supplier supplier = new Supplier();
        supplier.setName(dto.getName());
        supplier.setContactInfo(dto.getContactInfo());
        supplier.setAddress(dto.getAddress());
        supplier.setContractDetails(dto.getContractDetails());
        supplier.setRating(dto.getRating());

        // Handle product IDs - create placeholder products for service layer
        if (dto.getProductIds() != null && !dto.getProductIds().isEmpty()) {
            List<Product> placeholderProducts = dto.getProductIds().stream()
                    .map(productId -> {
                        Product product = new Product();
                        product.setId(productId);
                        return product;
                    })
                    .collect(Collectors.toList());
            supplier.setProducts(placeholderProducts);
        }

        return supplier;
    }
    /**
     * Convert Supplier entity to SupplierResponseDTO
     */
    public SupplierResponseDTO toResponseDTO(Supplier entity) {
        if (entity == null) return null;

        SupplierResponseDTO dto = new SupplierResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setContactInfo(entity.getContactInfo());
        dto.setAddress(entity.getAddress());
        dto.setContractDetails(entity.getContractDetails());
        dto.setRating(entity.getRating());
        dto.setCreatedAt(entity.getCreatedAt());

        // Map products if they exist
        if (entity.getProducts() != null) {
            List<ProductSummaryDTO> productSummaries = entity.getProducts().stream()
                    .map(this::toProductSummaryDTO)
                    .collect(Collectors.toList());
            dto.setProducts(productSummaries);
            dto.setTotalProducts(productSummaries.size());
        } else {
            dto.setTotalProducts(0);
        }

        return dto;
    }

    /**
     * Convert Supplier entity to SupplierSummaryDTO
     */
    public SupplierSummaryDTO toSummaryDTO(Supplier entity) {
        if (entity == null) return null;

        SupplierSummaryDTO dto = new SupplierSummaryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setContactInfo(entity.getContactInfo());
        dto.setAddress(entity.getAddress());
        dto.setRating(entity.getRating());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setTotalProducts(entity.getProducts() != null ? entity.getProducts().size() : 0);

        return dto;
    }

    /**
     * Convert Product entity to ProductSummaryDTO
     */
    public ProductSummaryDTO toProductSummaryDTO(Product entity) {
        if (entity == null) return null;

        ProductSummaryDTO dto = new ProductSummaryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSku(entity.getSku());
        dto.setPrice(entity.getPrice());
        dto.setStockQuantity(entity.getStock());
        dto.setCategory(entity.getCategories() != null ? entity.getCategories().toString() : null);
        dto.setIsActive(entity.getStatus() == ProductStatus.ACTIVE);

        return dto;
    }

    /**
     * Update existing Supplier entity from SupplierRequestDTO
     */
    public void updateEntityFromDTO(Supplier entity, SupplierRequestDTO dto) {
        if (entity == null || dto == null) return;

        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getContactInfo() != null) {
            entity.setContactInfo(dto.getContactInfo());
        }
        if (dto.getAddress() != null) {
            entity.setAddress(dto.getAddress());
        }
        if (dto.getContractDetails() != null) {
            entity.setContractDetails(dto.getContractDetails());
        }
        if (dto.getRating() != null) {
            entity.setRating(dto.getRating());
        }
    }

    /**
     * Convert list of Supplier entities to list of SupplierSummaryDTOs
     */
    public List<SupplierSummaryDTO> toSummaryDTOList(List<Supplier> entities) {
        if (entities == null) return null;

        return entities.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Supplier entities to list of SupplierResponseDTOs
     */
    public List<SupplierResponseDTO> toResponseDTOList(List<Supplier> entities) {
        if (entities == null) return null;

        return entities.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
}