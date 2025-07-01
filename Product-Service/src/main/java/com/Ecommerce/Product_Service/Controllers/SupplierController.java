package com.Ecommerce.Product_Service.Controllers;


import com.Ecommerce.Product_Service.Payload.Product.ProductSummaryDTO;
import com.Ecommerce.Product_Service.Payload.Supplier.*;
import com.Ecommerce.Product_Service.Services.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Collections;

@RestController
@RequestMapping("/suppliers")
@Validated
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierMapper supplierMapper;
    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers() {
        List<SupplierResponseDTO> suppliers = supplierService.findAllSuppliers()
                .stream()
                .map(supplierMapper::toResponseDTO)
                .collect(Collectors.toList());

        // Sort by creation date (newest first)
        Collections.reverse(suppliers);
        return ResponseEntity.ok(suppliers);
    }
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getSupplierById(@PathVariable UUID id) {
        return supplierService.findSupplierById(id)
                .map(supplierMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @Transactional(readOnly = true)
    @GetMapping("/search")
    public ResponseEntity<List<SupplierSummaryDTO>> searchSuppliersByName(
            @RequestParam String name) {
        List<SupplierSummaryDTO> suppliers = supplierService.findSuppliersByName(name)
                .stream()
                .map(supplierMapper::toSummaryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(suppliers);
    }
    @Transactional(readOnly = true)
    @GetMapping("/rating/{minRating}")
    public ResponseEntity<List<SupplierSummaryDTO>> getSuppliersByMinimumRating(
            @PathVariable
            @DecimalMin(value = "0.0", message = "Minimum rating cannot be negative")
            @DecimalMax(value = "5.0", message = "Minimum rating cannot exceed 5.0")
            BigDecimal minRating) {

        List<SupplierSummaryDTO> suppliers = supplierService.findSuppliersByMinimumRating(minRating)
                .stream()
                .map(supplierMapper::toSummaryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(suppliers);
    }

    @PostMapping
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @Valid @RequestBody SupplierRequestDTO supplierRequest) {
        try {
            var supplier = supplierMapper.toEntity(supplierRequest);
            var createdSupplier = supplierService.saveSupplier(supplier);
            var responseDTO = supplierMapper.toResponseDTO(createdSupplier);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating supplier");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequestDTO supplierRequest) {
        try {
            return supplierService.findSupplierById(id)
                    .map(existingSupplier -> {
                        supplierMapper.updateEntityFromDTO(existingSupplier, supplierRequest);
                        return supplierService.updateSupplierInfo(id, existingSupplier)
                                .map(supplierMapper::toResponseDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating supplier");
        }
    }

    @PatchMapping("/{id}/rating")
    public ResponseEntity<SupplierResponseDTO> rateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody RatingRequestDTO ratingRequest) {
        try {
            return supplierService.rateSupplier(id, ratingRequest.getRating())
                    .map(supplierMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error rating supplier");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable UUID id) {
        try {
            if (supplierService.findSupplierById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            supplierService.deleteSupplier(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting supplier");
        }
    }

    @PostMapping("/{supplierId}/products/{productId}")
    public ResponseEntity<SupplierResponseDTO> addProductToSupplier(
            @PathVariable UUID supplierId,
            @PathVariable UUID productId) {
        try {
            return supplierService.addProductToSupplier(supplierId, productId)
                    .map(supplierMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error adding product to supplier");
        }
    }

    @DeleteMapping("/{supplierId}/products/{productId}")
    public ResponseEntity<SupplierResponseDTO> removeProductFromSupplier(
            @PathVariable UUID supplierId,
            @PathVariable UUID productId) {
        try {
            return supplierService.removeProductFromSupplier(supplierId, productId)
                    .map(supplierMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error removing product from supplier");
        }
    }
    @Transactional(readOnly = true)
    @GetMapping("/{supplierId}/products")
    public ResponseEntity<List<ProductSummaryDTO>> getProductsBySupplier(@PathVariable UUID supplierId) {
        try {
            List<ProductSummaryDTO> products = supplierService.findProductsBySupplier(supplierId)
                    .stream()
                    .map(supplierMapper::toProductSummaryDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving products for supplier");
        }
    }
    @Transactional(readOnly = true)
    @GetMapping("/top-rated")
    public ResponseEntity<List<SupplierSummaryDTO>> getTopRatedSuppliers(
            @RequestParam(defaultValue = "10") int limit) {
        List<SupplierSummaryDTO> suppliers = supplierService.findAllSuppliers()
                .stream()
                .filter(supplier -> supplier.getRating() != null)
                .sorted((s1, s2) -> s2.getRating().compareTo(s1.getRating()))
                .limit(limit)
                .map(supplierMapper::toSummaryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(suppliers);
    }
    @Transactional(readOnly = true)
    @GetMapping("/stats")
    public ResponseEntity<SupplierStatsDTO> getSupplierStatistics() {
        try {
            var allSuppliers = supplierService.findAllSuppliers();

            SupplierStatsDTO stats = new SupplierStatsDTO();
            stats.setTotalSuppliers(allSuppliers.size());

            // Calculate total products across all suppliers
            long totalProducts = allSuppliers.stream()
                    .mapToLong(s -> s.getProducts() != null ? s.getProducts().size() : 0)
                    .sum();
            stats.setTotalProducts(totalProducts);

            // Calculate rating statistics
            List<BigDecimal> ratings = allSuppliers.stream()
                    .filter(s -> s.getRating() != null)
                    .map(s -> s.getRating())
                    .collect(Collectors.toList());

            if (!ratings.isEmpty()) {
                BigDecimal avgRating = ratings.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(ratings.size()), 2, BigDecimal.ROUND_HALF_UP);
                stats.setAverageRating(avgRating);

                stats.setHighestRating(ratings.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                stats.setLowestRating(ratings.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));

                // Find top rated supplier
                String topRatedSupplier = allSuppliers.stream()
                        .filter(s -> s.getRating() != null)
                        .max((s1, s2) -> s1.getRating().compareTo(s2.getRating()))
                        .map(s -> s.getName())
                        .orElse("None");
                stats.setTopRatedSupplierName(topRatedSupplier);
            } else {
                stats.setAverageRating(BigDecimal.ZERO);
                stats.setHighestRating(BigDecimal.ZERO);
                stats.setLowestRating(BigDecimal.ZERO);
                stats.setTopRatedSupplierName("None");
            }

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving supplier statistics");
        }
    }
    @Transactional(readOnly = true)
    // Additional endpoint for advanced search
    @GetMapping("/search/advanced")
    public ResponseEntity<List<SupplierSummaryDTO>> advancedSearch(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String contactInfo,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) BigDecimal minRating) {

        List<SupplierSummaryDTO> suppliers = supplierService.findAllSuppliers()
                .stream()
                .filter(supplier -> {
                    boolean matches = true;

                    if (name != null && !name.isEmpty()) {
                        matches = matches && supplier.getName().toLowerCase().contains(name.toLowerCase());
                    }

                    if (contactInfo != null && !contactInfo.isEmpty()) {
                        matches = matches && supplier.getContactInfo().toLowerCase().contains(contactInfo.toLowerCase());
                    }

                    if (address != null && !address.isEmpty()) {
                        matches = matches && supplier.getAddress().toLowerCase().contains(address.toLowerCase());
                    }

                    if (minRating != null) {
                        matches = matches && supplier.getRating() != null &&
                                supplier.getRating().compareTo(minRating) >= 0;
                    }

                    return matches;
                })
                .map(supplierMapper::toSummaryDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(suppliers);
    }

    // Bulk operations endpoint
    @PostMapping("/bulk")
    public ResponseEntity<List<SupplierResponseDTO>> createBulkSuppliers(
            @Valid @RequestBody List<SupplierRequestDTO> supplierRequests) {
        try {
            List<SupplierResponseDTO> createdSuppliers = supplierRequests.stream()
                    .map(supplierMapper::toEntity)
                    .map(supplier -> {
                        try {
                            return supplierService.saveSupplier(supplier);
                        } catch (Exception e) {
                            // Log error and skip this supplier
                            System.err.println("Failed to create supplier: " + e.getMessage());
                            return null;
                        }
                    })
                    .filter(supplier -> supplier != null)
                    .map(supplierMapper::toResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(createdSuppliers);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating bulk suppliers");
        }
    }
}