package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.Supplier;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Repositories.SupplierRepository;
import com.Ecommerce.Product_Service.Utlis.ContractDetailsHelper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Supplier> findAllSuppliers() {
        return supplierRepository.findAll();
    }
    @Transactional(readOnly = true)
    public Optional<Supplier> findSupplierById(UUID id) {
        return supplierRepository.findById(id);
    }
    @Transactional(readOnly = true)
    public List<Supplier> findSuppliersByName(String name) {
        return supplierRepository.findByNameContainingIgnoreCase(name);
    }
    @Transactional(readOnly = true)
    public List<Supplier> findSuppliersByMinimumRating(BigDecimal minRating) {
        return supplierRepository.findByRatingGreaterThanEqual(minRating);
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            supplier.setCreatedAt(LocalDateTime.now());
        }

        // Validate contract details if present
        validateContractDetails(supplier.getContractDetails());

        // Extract product IDs before saving supplier
        List<UUID> productIds = new ArrayList<>();
        if (supplier.getProducts() != null && !supplier.getProducts().isEmpty()) {
            productIds = supplier.getProducts().stream()
                    .map(Product::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        // Clear placeholder products to avoid cascade issues
        supplier.setProducts(new ArrayList<>());

        // Save supplier first
        Supplier savedSupplier = supplierRepository.save(supplier);

        // Handle product-supplier relationships
        if (!productIds.isEmpty()) {
            establishProductSupplierRelationships(savedSupplier, productIds);
            // Reload supplier with products
            savedSupplier = supplierRepository.findByIdWithProducts(savedSupplier.getId())
                    .orElse(savedSupplier);
        }

        return savedSupplier;
    }

    private void establishProductSupplierRelationships(Supplier supplier, List<UUID> productIds) {
        // Fetch actual products from database
        List<Product> products = productRepository.findAllById(productIds);

        // Validate all products exist
        if (products.size() != productIds.size()) {
            List<UUID> foundIds = products.stream().map(Product::getId).collect(Collectors.toList());
            List<UUID> missingIds = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new IllegalArgumentException("Products not found: " + missingIds);
        }

        // Establish many-to-many relationships
        for (Product product : products) {
            // Add supplier to product's suppliers list (if not already present)
            if (!product.getSuppliers().contains(supplier)) {
                product.getSuppliers().add(supplier);
            }

            // Add product to supplier's products list (if not already present)
            if (!supplier.getProducts().contains(product)) {
                supplier.getProducts().add(product);
            }
        }

        // Save products to update the join table
        productRepository.saveAll(products);


    }

    @Transactional
    public Optional<Supplier> updateSupplierInfo(UUID id, Supplier updatedSupplier) {
        return supplierRepository.findById(id)
                .map(existingSupplier -> {
                    if (updatedSupplier.getName() != null) {
                        existingSupplier.setName(updatedSupplier.getName());
                    }
                    if (updatedSupplier.getContactInfo() != null) {
                        existingSupplier.setContactInfo(updatedSupplier.getContactInfo());
                    }
                    if (updatedSupplier.getAddress() != null) {
                        existingSupplier.setAddress(updatedSupplier.getAddress());
                    }
                    if (updatedSupplier.getContractDetails() != null) {
                        // Validate new contract details before updating
                        validateContractDetails(updatedSupplier.getContractDetails());
                        existingSupplier.setContractDetails(updatedSupplier.getContractDetails());
                    }

                    return supplierRepository.save(existingSupplier);
                });
    }

    @Transactional
    public Optional<Supplier> rateSupplier(UUID id, BigDecimal rating) {
        if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(new BigDecimal("5")) > 0) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }

        return supplierRepository.findById(id)
                .map(supplier -> {
                    supplier.setRating(rating);
                    return supplierRepository.save(supplier);
                });
    }

    @Transactional
    public void deleteSupplier(UUID id) {
        supplierRepository.deleteById(id);
    }

    @Transactional
    public Optional<Supplier> addProductToSupplier(UUID supplierId, UUID productId) {
        Optional<Supplier> supplierOpt = supplierRepository.findById(supplierId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (supplierOpt.isPresent() && productOpt.isPresent()) {
            Supplier supplier = supplierOpt.get();
            Product product = productOpt.get();

            supplier.getProducts().add(product);
            product.getSuppliers().add(supplier);

            productRepository.save(product);
            return Optional.of(supplierRepository.save(supplier));
        }

        return Optional.empty();
    }

    @Transactional
    public Optional<Supplier> removeProductFromSupplier(UUID supplierId, UUID productId) {
        Optional<Supplier> supplierOpt = supplierRepository.findById(supplierId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (supplierOpt.isPresent() && productOpt.isPresent()) {
            Supplier supplier = supplierOpt.get();
            Product product = productOpt.get();

            supplier.getProducts().removeIf(p -> p.getId().equals(productId));
            product.getSuppliers().removeIf(s -> s.getId().equals(supplierId));

            productRepository.save(product);
            return Optional.of(supplierRepository.save(supplier));
        }

        return Optional.empty();
    }

    public List<Product> findProductsBySupplier(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .map(Supplier::getProducts)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    // NEW METHODS USING ContractDetailsHelper

    /**
     * Create a supplier with standard contract details
     */
    @Transactional
    public Supplier createSupplierWithStandardContract(
            String name,
            String contactInfo,
            String address,
            String contractType,
            BigDecimal contractValue,
            String paymentTerms,
            int contractDurationMonths) {

        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setContactInfo(contactInfo);
        supplier.setAddress(address);
        supplier.setCreatedAt(LocalDateTime.now());

        // Create contract details using helper
        Map<String, Object> contractDetails = ContractDetailsHelper.createContractDetails(
                contractType,
                LocalDateTime.now(),
                LocalDateTime.now().plusMonths(contractDurationMonths),
                contractValue,
                paymentTerms
        );

        supplier.setContractDetails(contractDetails);

        return supplierRepository.save(supplier);
    }

    /**
     * Update contract details for a supplier
     */
    @Transactional
    public Optional<Supplier> updateSupplierContract(
            UUID supplierId,
            String contractType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal contractValue,
            String paymentTerms) {

        return supplierRepository.findById(supplierId)
                .map(supplier -> {
                    Map<String, Object> newContractDetails = ContractDetailsHelper.createContractDetails(
                            contractType, startDate, endDate, contractValue, paymentTerms
                    );

                    validateContractDetails(newContractDetails);
                    supplier.setContractDetails(newContractDetails);

                    return supplierRepository.save(supplier);
                });
    }

    /**
     * Find suppliers with active contracts
     */
    public List<Supplier> findSuppliersWithActiveContracts() {
        return supplierRepository.findAll()
                .stream()
                .filter(supplier -> ContractDetailsHelper.isContractActive(supplier.getContractDetails()))
                .collect(Collectors.toList());
    }

    /**
     * Find suppliers with expired contracts
     */
    public List<Supplier> findSuppliersWithExpiredContracts() {
        LocalDateTime now = LocalDateTime.now();
        return supplierRepository.findAll()
                .stream()
                .filter(supplier -> {
                    Map<String, Object> contractDetails = supplier.getContractDetails();
                    if (contractDetails == null) {
                        return false;
                    }

                    LocalDateTime endDate = ContractDetailsHelper.getContractDetail(
                            contractDetails, "endDate", LocalDateTime.class
                    );

                    return endDate != null && endDate.isBefore(now);
                })
                .collect(Collectors.toList());
    }

    /**
     * Find suppliers with contracts expiring soon (within specified days)
     */
    public List<Supplier> findSuppliersWithContractsExpiringSoon(int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(daysAhead);

        return supplierRepository.findAll()
                .stream()
                .filter(supplier -> {
                    Map<String, Object> contractDetails = supplier.getContractDetails();
                    if (contractDetails == null) {
                        return false;
                    }

                    LocalDateTime endDate = ContractDetailsHelper.getContractDetail(
                            contractDetails, "endDate", LocalDateTime.class
                    );

                    return endDate != null &&
                            endDate.isAfter(now) &&
                            endDate.isBefore(threshold);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get contract value for a supplier
     */
    public BigDecimal getSupplierContractValue(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .map(supplier -> {
                    Map<String, Object> contractDetails = supplier.getContractDetails();
                    return ContractDetailsHelper.getContractDetail(
                            contractDetails, "contractValue", BigDecimal.class
                    );
                })
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get total contract value across all suppliers
     */
    public BigDecimal getTotalContractValue() {
        return supplierRepository.findAll()
                .stream()
                .map(supplier -> {
                    Map<String, Object> contractDetails = supplier.getContractDetails();
                    BigDecimal contractValue = ContractDetailsHelper.getContractDetail(
                            contractDetails, "contractValue", BigDecimal.class
                    );
                    return contractValue != null ? contractValue : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if supplier has an active contract
     */
    public boolean hasActiveContract(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .map(supplier -> ContractDetailsHelper.isContractActive(supplier.getContractDetails()))
                .orElse(false);
    }

    /**
     * Get contract type for a supplier
     */
    public String getSupplierContractType(UUID supplierId) {
        return supplierRepository.findById(supplierId)
                .map(supplier -> {
                    Map<String, Object> contractDetails = supplier.getContractDetails();
                    return ContractDetailsHelper.getContractDetail(
                            contractDetails, "contractType", String.class
                    );
                })
                .orElse("No Contract");
    }

    /**
     * Find suppliers by contract type
     */
    public List<Supplier> findSuppliersByContractType(String contractType) {
        return supplierRepository.findAll()
                .stream()
                .filter(supplier -> {
                    String supplierContractType = ContractDetailsHelper.getContractDetail(
                            supplier.getContractDetails(), "contractType", String.class
                    );
                    return contractType.equals(supplierContractType);
                })
                .collect(Collectors.toList());
    }

    /**
     * Validate contract details using helper methods
     */
    private void validateContractDetails(Map<String, Object> contractDetails) {
        if (contractDetails == null) {
            return; // Contract details are optional
        }

        // Validate start and end dates
        LocalDateTime startDate = ContractDetailsHelper.getContractDetail(
                contractDetails, "startDate", LocalDateTime.class
        );
        LocalDateTime endDate = ContractDetailsHelper.getContractDetail(
                contractDetails, "endDate", LocalDateTime.class
        );

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Contract start date cannot be after end date");
            }
        }

        // Validate contract value
        BigDecimal contractValue = ContractDetailsHelper.getContractDetail(
                contractDetails, "contractValue", BigDecimal.class
        );

        if (contractValue != null && contractValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Contract value cannot be negative");
        }

        // Validate contract type
        String contractType = ContractDetailsHelper.getContractDetail(
                contractDetails, "contractType", String.class
        );

        if (contractType != null && contractType.trim().isEmpty()) {
            throw new IllegalArgumentException("Contract type cannot be empty");
        }
    }

    /**
     * Extend contract for a supplier
     */
    @Transactional
    public Optional<Supplier> extendSupplierContract(UUID supplierId, int additionalMonths) {
        return supplierRepository.findById(supplierId)
                .map(supplier -> {
                    Map<String, Object> contractDetails = supplier.getContractDetails();
                    if (contractDetails == null) {
                        throw new IllegalArgumentException("Supplier has no contract to extend");
                    }

                    LocalDateTime currentEndDate = ContractDetailsHelper.getContractDetail(
                            contractDetails, "endDate", LocalDateTime.class
                    );

                    if (currentEndDate == null) {
                        throw new IllegalArgumentException("Contract has no end date to extend");
                    }

                    // Extend the contract
                    LocalDateTime newEndDate = currentEndDate.plusMonths(additionalMonths);
                    contractDetails.put("endDate", newEndDate);

                    supplier.setContractDetails(contractDetails);
                    return supplierRepository.save(supplier);
                });
    }
}