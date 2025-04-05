package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Discount;
import com.Ecommerce.Product_Service.Entities.DiscountType;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Repositories.DiscountRepository;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiscountService {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Discount> findAllDiscounts() {
        return discountRepository.findAll();
    }

    public Optional<Discount> findDiscountById(UUID id) {
        return discountRepository.findById(id);
    }

    public List<Discount> findDiscountsByProductId(UUID productId) {
        return discountRepository.findByProductId(productId);
    }

    public List<Discount> findDiscountsByType(DiscountType discountType) {
        return discountRepository.findByDiscountType(discountType);
    }

    public List<Discount> findActiveDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        return discountRepository.findByStartDateBeforeAndEndDateAfter(now, now);
    }

    @Transactional
    public Optional<Discount> createDiscount(UUID productId, Discount discount) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return Optional.empty();
        }

        Product product = productOpt.get();
        discount.setProduct(product);

        // Validate discount
        validateDiscount(discount);

        // Save the discount
        Discount savedDiscount = discountRepository.save(discount);
        product.getDiscounts().add(savedDiscount);
        productRepository.save(product);

        return Optional.of(savedDiscount);
    }

    @Transactional
    public Optional<Discount> updateDiscount(UUID id, Discount updatedDiscount) {
        return discountRepository.findById(id)
                .map(existingDiscount -> {
                    // Update fields if provided
                    if (updatedDiscount.getDiscountType() != null) {
                        existingDiscount.setDiscountType(updatedDiscount.getDiscountType());
                    }
                    if (updatedDiscount.getDiscountValue() != null) {
                        existingDiscount.setDiscountValue(updatedDiscount.getDiscountValue());
                    }
                    if (updatedDiscount.getStartDate() != null) {
                        existingDiscount.setStartDate(updatedDiscount.getStartDate());
                    }
                    if (updatedDiscount.getEndDate() != null) {
                        existingDiscount.setEndDate(updatedDiscount.getEndDate());
                    }
                    if (updatedDiscount.getMinPurchaseAmount() != null) {
                        existingDiscount.setMinPurchaseAmount(updatedDiscount.getMinPurchaseAmount());
                    }
                    if (updatedDiscount.getMaxDiscountAmount() != null) {
                        existingDiscount.setMaxDiscountAmount(updatedDiscount.getMaxDiscountAmount());
                    }

                    // Validate updated discount
                    validateDiscount(existingDiscount);

                    return discountRepository.save(existingDiscount);
                });
    }

    @Transactional
    public void deleteDiscount(UUID id) {
        discountRepository.findById(id).ifPresent(discount -> {
            Product product = discount.getProduct();
            product.getDiscounts().removeIf(d -> d.getId().equals(id));
            productRepository.save(product);
            discountRepository.deleteById(id);
        });
    }

    public BigDecimal calculateFinalPrice(UUID productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }

        Product product = productOpt.get();
        BigDecimal originalPrice = product.getPrice();

        // Find active discounts for this product
        LocalDateTime now = LocalDateTime.now();
        List<Discount> activeDiscounts = discountRepository.findByProductId(product.getId()).stream()
                .filter(d -> d.getStartDate().isBefore(now) && d.getEndDate().isAfter(now))
                .toList();

        // Apply the best discount
        if (activeDiscounts.isEmpty()) {
            return originalPrice;
        }

        // For simplicity, find the discount that results in the lowest price
        BigDecimal bestPrice = originalPrice;
        for (Discount discount : activeDiscounts) {
            BigDecimal priceAfterDiscount = applyDiscount(originalPrice, discount);
            if (priceAfterDiscount.compareTo(bestPrice) < 0) {
                bestPrice = priceAfterDiscount;
            }
        }

        return bestPrice;
    }

    private BigDecimal applyDiscount(BigDecimal originalPrice, Discount discount) {
        if (originalPrice.compareTo(discount.getMinPurchaseAmount()) < 0) {
            return originalPrice; // Minimum purchase amount not met
        }

        BigDecimal discountedPrice = originalPrice;

        switch (discount.getDiscountType()) {
            case PERCENTAGE:
                BigDecimal discountAmount = originalPrice.multiply(discount.getDiscountValue().divide(new BigDecimal("100")));

                // Apply max discount limit if specified
                if (discount.getMaxDiscountAmount() != null &&
                        discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
                    discountAmount = discount.getMaxDiscountAmount();
                }

                discountedPrice = originalPrice.subtract(discountAmount);
                break;

            case FIXED_AMOUNT:
                discountedPrice = originalPrice.subtract(discount.getDiscountValue());
                break;

            case BUY_ONE_GET_ONE:
                // For BOGO, we effectively discount by 50% for every two items
                discountedPrice = originalPrice.multiply(new BigDecimal("0.5"));
                break;
        }

        // Ensure price doesn't go below zero
        return discountedPrice.max(BigDecimal.ZERO);
    }

    private void validateDiscount(Discount discount) {
        // Basic validation
        if (discount.getStartDate() != null && discount.getEndDate() != null &&
                discount.getStartDate().isAfter(discount.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        if (discount.getDiscountValue() != null && discount.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discount value cannot be negative");
        }

        // Type-specific validation
        if (discount.getDiscountType() == DiscountType.PERCENTAGE &&
                discount.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }
    }
}
