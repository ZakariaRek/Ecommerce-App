package com.Ecommerce.Product_Service.Listener;



import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Services.Kakfa.CategoryEventService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CategoryEntityListener {

    private static CategoryEventService categoryEventService;

    @Autowired
    public void setCategoryEventService(CategoryEventService categoryEventService) {
        CategoryEntityListener.categoryEventService = categoryEventService;
    }

    @PostPersist
    public void postPersist(Category category) {
        if (categoryEventService != null) {
            categoryEventService.publishCategoryCreatedEvent(category);
        }
    }

    @PostUpdate
    public void postUpdate(Category category) {
        if (categoryEventService != null) {
            // Basic update event
            categoryEventService.publishCategoryUpdatedEvent(category);

            // The @PostUpdate doesn't provide the previous state
            // In a real application, you would need to use @PreUpdate to capture previous values
            // and store them in thread-local variables or a similar mechanism
        }
    }

    @PostRemove
    public void postRemove(Category category) {
        if (categoryEventService != null) {
            categoryEventService.publishCategoryDeletedEvent(category);
        }
    }

    @PreUpdate
    public void preUpdate(Category category) {
        // In a real application, you would store the current state before the update
        // For example, using a ThreadLocal variable or similar mechanism
        // ThreadLocalContext.setPreviousParentId(category.getParentId());
        // ThreadLocalContext.setPreviousImageUrl(category.getImageUrl());
        // ThreadLocalContext.setPreviousLevel(category.getLevel());
        // ThreadLocalContext.setPreviousFullPath(category.getFullPath());
    }
}