package com.Ecommerce.Product_Service.Listener;

import com.Ecommerce.Product_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Events.ProductEvents;
import com.Ecommerce.Product_Service.Services.Kakfa.ProductEventService;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ProductEntityListener {

    // We can't directly autowire in JPA EntityListener
    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        ProductEntityListener.applicationContext = applicationContext;
    }

    private ProductEventService getProductEventService() {
        return applicationContext.getBean(ProductEventService.class);
    }

    private KafkaTemplate<String, Object> getKafkaTemplate() {
        return applicationContext.getBean(KafkaTemplate.class);
    }

    @PostPersist
    public void postPersist(Product product) {
        log.info("Product created event triggered for product: {}", product.getId());

        // Get the event from the service
        ProductEvents.ProductCreatedEvent event = getProductEventService().createProductCreatedEvent(product);

        // Send the event
        String key = (product.getId() != null) ? product.getId().toString() : "pending-id";
        sendMessage(KafkaProducerConfig.TOPIC_PRODUCT_CREATED, key, event);
    }

    @PostUpdate
    public void postUpdate(Product product) {
        log.info("Product updated event triggered for product: {}", product.getId());

        // Main update event
        ProductEvents.ProductUpdatedEvent event = getProductEventService().createProductUpdatedEvent(product);
        sendMessage(KafkaProducerConfig.TOPIC_PRODUCT_UPDATED, product.getId().toString(), event);

        // Specific events based on what changed
        checkAndPublishPriceChangedEvent(product);
        checkAndPublishStockChangedEvent(product);
        checkAndPublishStatusChangedEvent(product);
    }

    @PreRemove
    public void preRemove(Product product) {
        log.info("Product deleted event triggered for product: {}", product.getId());

        ProductEvents.ProductDeletedEvent event = getProductEventService().createProductDeletedEvent(product.getId());
        sendMessage(KafkaProducerConfig.TOPIC_PRODUCT_DELETED, product.getId().toString(), event);
    }

    private void checkAndPublishPriceChangedEvent(Product product) {
        BigDecimal previousPrice = product.getPreviousPrice();
        if (previousPrice != null && !previousPrice.equals(product.getPrice())) {
            log.info("Price changed from {} to {} for product: {}",
                    previousPrice, product.getPrice(), product.getId());

            ProductEvents.ProductPriceChangedEvent event = getProductEventService()
                    .createPriceChangedEvent(product, previousPrice);
            sendMessage(KafkaProducerConfig.TOPIC_PRODUCT_PRICE_CHANGED, product.getId().toString(), event);
        }
    }

    private void checkAndPublishStockChangedEvent(Product product) {
        Integer previousStock = product.getPreviousStock();
        if (previousStock != null && !Objects.equals(previousStock, product.getStock())) {
            log.info("Stock changed from {} to {} for product: {}",
                    previousStock, product.getStock(), product.getId());

            ProductEvents.ProductStockChangedEvent event = getProductEventService()
                    .createStockChangedEvent(product, previousStock);
            sendMessage(KafkaProducerConfig.TOPIC_PRODUCT_STOCK_CHANGED, product.getId().toString(), event);
        }
    }

    private void checkAndPublishStatusChangedEvent(Product product) {
        ProductStatus previousStatus = product.getPreviousStatus();
        if (previousStatus != null && !previousStatus.equals(product.getStatus())) {
            log.info("Status changed from {} to {} for product: {}",
                    previousStatus, product.getStatus(), product.getId());

            ProductEvents.ProductStatusChangedEvent event = getProductEventService()
                    .createStatusChangedEvent(product, previousStatus);
            sendMessage(KafkaProducerConfig.TOPIC_PRODUCT_STATUS_CHANGED, product.getId().toString(), event);
        }
    }

    // Generic method to send Kafka messages
    private <T> void sendMessage(String topic, String key, T event) {
        log.info("Publishing event to topic: {} for key: {}", topic, key);

        CompletableFuture<SendResult<String, Object>> future =
                getKafkaTemplate().send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event sent successfully to topic: {} for key: {}", topic, key);
            } else {
                log.error("Failed to send event to topic: {} for key: {}", topic, key, ex);
            }
        });
    }
}