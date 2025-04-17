package com.Ecommerce.Product_Service.Services.Kakfa;



import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Entities.Supplier;
import com.Ecommerce.Product_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Product_Service.Events.ProductEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishProductCreatedEvent(Product product) {
        ProductEvents.ProductCreatedEvent event = mapToCreatedEvent(product);
        String key = product.getId().toString();

        log.info("Publishing product created event for product ID: {}", key);
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_PRODUCT_CREATED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Product created event sent successfully for product ID: {}", key);
            } else {
                log.error("Failed to send product created event for product ID: {}", key, ex);
            }
        });
    }

    public void publishProductUpdatedEvent(Product product) {
        ProductEvents.ProductUpdatedEvent event = mapToUpdatedEvent(product);
        String key = product.getId().toString();

        log.info("Publishing product updated event for product ID: {}", key);
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_PRODUCT_UPDATED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Product updated event sent successfully for product ID: {}", key);
            } else {
                log.error("Failed to send product updated event for product ID: {}", key, ex);
            }
        });
    }

    /**
     * Publishes a product deleted event to Kafka.
     * This method only requires the product ID, not the entire product object.
     *
     * @param productId the ID of the deleted product
     */
    public void publishProductDeletedEvent(UUID productId) {
        ProductEvents.ProductDeletedEvent event = ProductEvents.ProductDeletedEvent.builder()
                .productId(productId)
                .deletedAt(LocalDateTime.now())
                .build();

        String key = productId.toString();

        log.info("Publishing product deleted event for product ID: {}", key);
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_PRODUCT_DELETED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Product deleted event sent successfully for product ID: {}", key);
            } else {
                log.error("Failed to send product deleted event for product ID: {}", key, ex);
            }
        });
    }

    public void publishStockChangedEvent(Product product, Integer previousStock) {
        ProductEvents.ProductStockChangedEvent event = ProductEvents.ProductStockChangedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .previousStock(previousStock)
                .newStock(product.getStock())
                .updatedAt(LocalDateTime.now())
                .build();

        String key = product.getId().toString();

        log.info("Publishing stock changed event for product ID: {}", key);
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_PRODUCT_STOCK_CHANGED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Stock changed event sent successfully for product ID: {}", key);
            } else {
                log.error("Failed to send stock changed event for product ID: {}", key, ex);
            }
        });
    }

    public void publishPriceChangedEvent(Product product, BigDecimal previousPrice) {
        ProductEvents.ProductPriceChangedEvent event = ProductEvents.ProductPriceChangedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .previousPrice(previousPrice)
                .newPrice(product.getPrice())
                .updatedAt(LocalDateTime.now())
                .build();

        String key = product.getId().toString();

        log.info("Publishing price changed event for product ID: {}", key);
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_PRODUCT_PRICE_CHANGED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Price changed event sent successfully for product ID: {}", key);
            } else {
                log.error("Failed to send price changed event for product ID: {}", key, ex);
            }
        });
    }

    public void publishStatusChangedEvent(Product product, ProductStatus previousStatus) {
        ProductEvents.ProductStatusChangedEvent event = ProductEvents.ProductStatusChangedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .previousStatus(previousStatus)
                .newStatus(product.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();

        String key = product.getId().toString();

        log.info("Publishing status changed event for product ID: {}", key);
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaProducerConfig.TOPIC_PRODUCT_STATUS_CHANGED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Status changed event sent successfully for product ID: {}", key);
            } else {
                log.error("Failed to send status changed event for product ID: {}", key, ex);
            }
        });
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