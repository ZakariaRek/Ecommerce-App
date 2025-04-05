package com.Ecommerce.Product_Service.Services;


import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.Supplier;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Repositories.SupplierRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Supplier> findAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> findSupplierById(UUID id) {
        return supplierRepository.findById(id);
    }

    public List<Supplier> findSuppliersByName(String name) {
        return supplierRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Supplier> findSuppliersByMinimumRating(BigDecimal minRating) {
        return supplierRepository.findByRatingGreaterThanEqual(minRating);
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getId() == null) {
            supplier.setCreatedAt(LocalDateTime.now());
        }
        return supplierRepository.save(supplier);
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
}
