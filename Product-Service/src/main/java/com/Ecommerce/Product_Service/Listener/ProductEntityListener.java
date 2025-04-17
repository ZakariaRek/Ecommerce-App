package com.Ecommerce.Product_Service.Listener;



import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Services.Kakfa.ProductEventService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

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

    @PostPersist
    public void postPersist(Product product) {
        log.info("Product created event triggered for product: {}", product.getId());
        getProductEventService().publishProductCreatedEvent(product);
    }

    @PostUpdate
    public void postUpdate(Product product) {
        log.info("Product updated event triggered for product: {}", product.getId());

        // Main update event
        getProductEventService().publishProductUpdatedEvent(product);

        // Specific events based on what changed
        checkAndPublishPriceChangedEvent(product);
        checkAndPublishStockChangedEvent(product);
        checkAndPublishStatusChangedEvent(product);
    }

    @PreRemove
    public void preRemove(Product product) {
        log.info("Product deleted event triggered for product: {}", product.getId());
        getProductEventService().publishProductDeletedEvent(product.getId());
    }

    private void checkAndPublishPriceChangedEvent(Product product) {
        BigDecimal previousPrice = product.getPreviousPrice();
        if (previousPrice != null && !previousPrice.equals(product.getPrice())) {
            log.info("Price changed from {} to {} for product: {}",
                    previousPrice, product.getPrice(), product.getId());
            getProductEventService().publishPriceChangedEvent(product, previousPrice);
        }
    }

    private void checkAndPublishStockChangedEvent(Product product) {
        Integer previousStock = product.getPreviousStock();
        if (previousStock != null && !Objects.equals(previousStock, product.getStock())) {
            log.info("Stock changed from {} to {} for product: {}",
                    previousStock, product.getStock(), product.getId());
            getProductEventService().publishStockChangedEvent(product, previousStock);
        }
    }

    private void checkAndPublishStatusChangedEvent(Product product) {
        ProductStatus previousStatus = product.getPreviousStatus();
        if (previousStatus != null && !previousStatus.equals(product.getStatus())) {
            log.info("Status changed from {} to {} for product: {}",
                    previousStatus, product.getStatus(), product.getId());
            getProductEventService().publishStatusChangedEvent(product, previousStatus);
        }
    }
}