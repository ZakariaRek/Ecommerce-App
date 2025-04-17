package com.Ecommerce.Product_Service.Services.Kakfa;

import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Entities.Supplier;
import com.Ecommerce.Product_Service.Events.ProductEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventService {

    // This service now only creates events without sending them

    public ProductEvents.ProductCreatedEvent createProductCreatedEvent(Product product) {
        log.info("Creating product created event for product ID: {}",
                (product.getId() != null) ? product.getId().toString() : "pending-id");
        return mapToCreatedEvent(product);
    }

    public ProductEvents.ProductUpdatedEvent createProductUpdatedEvent(Product product) {
        log.info("Creating product updated event for product ID: {}", product.getId());
        return mapToUpdatedEvent(product);
    }

    public ProductEvents.ProductDeletedEvent createProductDeletedEvent(UUID productId) {
        log.info("Creating product deleted event for product ID: {}", productId);
        return ProductEvents.ProductDeletedEvent.builder()
                .productId(productId)
                .deletedAt(LocalDateTime.now())
                .build();
    }

    public ProductEvents.ProductStockChangedEvent createStockChangedEvent(Product product, Integer previousStock) {
        log.info("Creating stock changed event for product ID: {}", product.getId());
        return ProductEvents.ProductStockChangedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .previousStock(previousStock)
                .newStock(product.getStock())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public ProductEvents.ProductPriceChangedEvent createPriceChangedEvent(Product product, BigDecimal previousPrice) {
        log.info("Creating price changed event for product ID: {}", product.getId());
        return ProductEvents.ProductPriceChangedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .previousPrice(previousPrice)
                .newPrice(product.getPrice())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public ProductEvents.ProductStatusChangedEvent createStatusChangedEvent(Product product, ProductStatus previousStatus) {
        log.info("Creating status changed event for product ID: {}", product.getId());
        return ProductEvents.ProductStatusChangedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .previousStatus(previousStatus)
                .newStatus(product.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // Helper methods to map Product entity to event objects
    private ProductEvents.ProductCreatedEvent mapToCreatedEvent(Product product) {
        return ProductEvents.ProductCreatedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .sku(product.getSku())
                .weight(product.getWeight())
                .dimensions(product.getDimensions())
                .images(product.getImages())
                .status(product.getStatus())
                .categoryIds(mapToCategoryIds(product.getCategories()))
                .supplierIds(mapToSupplierIds(product.getSuppliers()))
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductEvents.ProductUpdatedEvent mapToUpdatedEvent(Product product) {
        return ProductEvents.ProductUpdatedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .sku(product.getSku())
                .weight(product.getWeight())
                .dimensions(product.getDimensions())
                .images(product.getImages())
                .status(product.getStatus())
                .categoryIds(mapToCategoryIds(product.getCategories()))
                .supplierIds(mapToSupplierIds(product.getSuppliers()))
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private List<UUID> mapToCategoryIds(List<Category> categories) {
        return categories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());
    }

    private List<UUID> mapToSupplierIds(List<Supplier> suppliers) {
        return suppliers.stream()
                .map(Supplier::getId)
                .collect(Collectors.toList());
    }
}