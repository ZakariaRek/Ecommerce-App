package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.Supplier;
import com.Ecommerce.Product_Service.Services.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.findAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplierById(@PathVariable UUID id) {
        return supplierService.findSupplierById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Supplier>> searchSuppliersByName(@RequestParam String name) {
        return ResponseEntity.ok(supplierService.findSuppliersByName(name));
    }

    @GetMapping("/rating/{minRating}")
    public ResponseEntity<List<Supplier>> getSuppliersByMinimumRating(@PathVariable BigDecimal minRating) {
        return ResponseEntity.ok(supplierService.findSuppliersByMinimumRating(minRating));
    }

    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        Supplier createdSupplier = supplierService.saveSupplier(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSupplier);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable UUID id, @RequestBody Supplier supplier) {
        return supplierService.updateSupplierInfo(id, supplier)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/rate")
    public ResponseEntity<Supplier> rateSupplier(@PathVariable UUID id, @RequestBody BigDecimal rating) {
        try {
            return supplierService.rateSupplier(id, rating)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable UUID id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{supplierId}/products/{productId}")
    public ResponseEntity<Supplier> addProductToSupplier(
            @PathVariable UUID supplierId,
            @PathVariable UUID productId) {
        return supplierService.addProductToSupplier(supplierId, productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{supplierId}/products/{productId}")
    public ResponseEntity<Supplier> removeProductFromSupplier(
            @PathVariable UUID supplierId,
            @PathVariable UUID productId) {
        return supplierService.removeProductFromSupplier(supplierId, productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{supplierId}/products")
    public ResponseEntity<List<Product>> getProductsBySupplier(@PathVariable UUID supplierId) {
        try {
            List<Product> products = supplierService.findProductsBySupplier(supplierId);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}

