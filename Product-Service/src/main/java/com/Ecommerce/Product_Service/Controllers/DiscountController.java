package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Discount;
import com.Ecommerce.Product_Service.Entities.DiscountType;
import com.Ecommerce.Product_Service.Services.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/discounts")
public class DiscountController {

    @Autowired
    private DiscountService discountService;

    @GetMapping
    public ResponseEntity<List<Discount>> getAllDiscounts() {
        return ResponseEntity.ok(discountService.findAllDiscounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Discount> getDiscountById(@PathVariable UUID id) {
        return discountService.findDiscountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Discount>> getDiscountsByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(discountService.findDiscountsByProductId(productId));
    }

    @GetMapping("/type/{discountType}")
    public ResponseEntity<List<Discount>> getDiscountsByType(@PathVariable DiscountType discountType) {
        return ResponseEntity.ok(discountService.findDiscountsByType(discountType));
    }

    @GetMapping("/active")
    public ResponseEntity<List<Discount>> getActiveDiscounts() {
        return ResponseEntity.ok(discountService.findActiveDiscounts());
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<Discount> createDiscount(
            @PathVariable UUID productId,
            @RequestBody Discount discount) {
        try {
            return discountService.createDiscount(productId, discount)
                    .map(createdDiscount -> ResponseEntity.status(HttpStatus.CREATED).body(createdDiscount))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Discount> updateDiscount(
            @PathVariable UUID id,
            @RequestBody Discount discount) {
        try {
            return discountService.updateDiscount(id, discount)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscount(@PathVariable UUID id) {
        discountService.deleteDiscount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/product/{productId}/final-price")
    public ResponseEntity<Map<String, BigDecimal>> calculateFinalPrice(@PathVariable UUID productId) {
        try {
            BigDecimal finalPrice = discountService.calculateFinalPrice(productId);
            return ResponseEntity.ok(Map.of("finalPrice", finalPrice));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}