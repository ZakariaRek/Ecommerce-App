package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Payload.Discont.DiscountRequestDTO;
import com.Ecommerce.Product_Service.Entities.DiscountType;
import com.Ecommerce.Product_Service.Payload.Discont.DiscountMapper;
import com.Ecommerce.Product_Service.Payload.Discont.DiscountResponseDTO;
import com.Ecommerce.Product_Service.Payload.Discont.PricingResponseDTO;
import com.Ecommerce.Product_Service.Payload.Discont.DiscountSummaryDTO;
import com.Ecommerce.Product_Service.Services.DiscountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Collections;

@RestController
@RequestMapping("/discounts")
public class DiscountController {

    @Autowired
    private DiscountService discountService;

    @Autowired
    private DiscountMapper discountMapper;

    @GetMapping
    public ResponseEntity<List<DiscountSummaryDTO>> getAllDiscounts() {
        // Fix 1: Proper way to reverse a stream and collect
        List<DiscountSummaryDTO> discounts = discountService.findAllDiscounts()
                .stream()
                .map(discountMapper::toSummaryDTO)
                .collect(Collectors.toList());

        // Reverse the list in place
        Collections.reverse(discounts);
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountResponseDTO> getDiscountById(@PathVariable UUID id) {
        return discountService.findDiscountById(id)
                .map(discountMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<DiscountSummaryDTO>> getDiscountsByProductId(@PathVariable UUID productId) {
        List<DiscountSummaryDTO> discounts = discountService.findDiscountsByProductId(productId)
                .stream()
                .map(discountMapper::toSummaryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/type/{discountType}")
    public ResponseEntity<List<DiscountSummaryDTO>> getDiscountsByType(@PathVariable DiscountType discountType) {
        List<DiscountSummaryDTO> discounts = discountService.findDiscountsByType(discountType)
                .stream()
                .map(discountMapper::toSummaryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/active")
    public ResponseEntity<List<DiscountSummaryDTO>> getActiveDiscounts() {
        List<DiscountSummaryDTO> discounts = discountService.findActiveDiscounts()
                .stream()
                .map(discountMapper::toSummaryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(discounts);
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<DiscountResponseDTO> createDiscount(
            @PathVariable UUID productId,
            @Valid @RequestBody DiscountRequestDTO discountRequest) {
        try {
            return discountService.createDiscount(productId, discountMapper.toEntity(discountRequest))
                    .map(discountMapper::toResponseDTO)
                    .map(createdDiscount -> ResponseEntity.status(HttpStatus.CREATED).body(createdDiscount))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating discount");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponseDTO> updateDiscount(
            @PathVariable UUID id,
            @Valid @RequestBody DiscountRequestDTO discountRequest) {
        try {
            return discountService.findDiscountById(id)
                    .map(existingDiscount -> {
                        discountMapper.updateEntityFromDTO(existingDiscount, discountRequest);
                        return discountService.updateDiscount(id, existingDiscount)
                                .map(discountMapper::toResponseDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating discount");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscount(@PathVariable UUID id) {
        try {
            if (discountService.findDiscountById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            discountService.deleteDiscount(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting discount");
        }
    }

    @GetMapping("/product/{productId}/pricing")
    public ResponseEntity<PricingResponseDTO> calculateFinalPrice(@PathVariable UUID productId) {
        try {
            PricingResponseDTO pricing = discountService.calculatePricingDetails(productId);
            return ResponseEntity.ok(pricing);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error calculating pricing");
        }
    }

    // Fix 2: Better handling of Optional in stream operations
    @PostMapping("/product/{productId}/bulk")
    public ResponseEntity<List<DiscountResponseDTO>> createBulkDiscounts(
            @PathVariable UUID productId,
            @Valid @RequestBody List<DiscountRequestDTO> discountRequests) {
        try {
            // Option 1: Using flatMap with Optional.stream() (Java 9+)
            List<DiscountResponseDTO> createdDiscounts = discountRequests.stream()
                    .map(discountMapper::toEntity)
                    .map(discount -> discountService.createDiscount(productId, discount))
                    .flatMap(optional -> optional.stream()) // Better than filter + map
                    .map(discountMapper::toResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(createdDiscounts);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating bulk discounts");
        }
    }

    // Alternative implementation for bulk operations if you prefer the filter approach
    @PostMapping("/product/{productId}/bulk-alt")
    public ResponseEntity<List<DiscountResponseDTO>> createBulkDiscountsAlternative(
            @PathVariable UUID productId,
            @Valid @RequestBody List<DiscountRequestDTO> discountRequests) {
        try {
            // Option 2: More explicit approach with better error handling
            List<DiscountResponseDTO> createdDiscounts = discountRequests.stream()
                    .map(discountMapper::toEntity)
                    .map(discount -> {
                        try {
                            return discountService.createDiscount(productId, discount);
                        } catch (Exception e) {
                            // Log the error for this specific discount
                            System.err.println("Failed to create discount: " + e.getMessage());
                            return java.util.Optional.<com.Ecommerce.Product_Service.Entities.Discount>empty();
                        }
                    })
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .map(discountMapper::toResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED).body(createdDiscounts);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating bulk discounts");
        }
    }

}
