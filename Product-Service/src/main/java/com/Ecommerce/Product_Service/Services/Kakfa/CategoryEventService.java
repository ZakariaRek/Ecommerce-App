package com.Ecommerce.Product_Service.Services.Kakfa;
import com.Ecommerce.Product_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Events.CategoryEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCategoryCreatedEvent(Category category) {
        try {
            CategoryEvents.CategoryCreatedEvent event = CategoryEvents.CategoryCreatedEvent.builder()
                    .categoryId(category.getId())
                    .name(category.getName())
                    .parentId(category.getParentId())
                    .description(category.getDescription())
                    .imageUrl(category.getImageUrl())
                    .level(category.getLevel())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing category created event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_CATEGORY_CREATED, category.getId().toString(), event);
            log.info("Category created event published successfully for category ID: {}", category.getId());
        } catch (Exception e) {
            log.error("Failed to publish category created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish category created event", e);
        }
    }

    public void publishCategoryUpdatedEvent(Category category) {
        try {
            CategoryEvents.CategoryUpdatedEvent event = CategoryEvents.CategoryUpdatedEvent.builder()
                    .categoryId(category.getId())
                    .name(category.getName())
                    .parentId(category.getParentId())
                    .description(category.getDescription())
                    .imageUrl(category.getImageUrl())
                    .level(category.getLevel())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing category updated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_CATEGORY_UPDATED, category.getId().toString(), event);
            log.info("Category updated event published successfully for category ID: {}", category.getId());
        } catch (Exception e) {
            log.error("Failed to publish category updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish category updated event", e);
        }
    }

    public void publishCategoryDeletedEvent(Category category) {
        try {
            CategoryEvents.CategoryDeletedEvent event = CategoryEvents.CategoryDeletedEvent.builder()
                    .categoryId(category.getId())
                    .name(category.getName())
                    .parentId(category.getParentId())
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing category deleted event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_CATEGORY_DELETED, category.getId().toString(), event);
            log.info("Category deleted event published successfully for category ID: {}", category.getId());
        } catch (Exception e) {
            log.error("Failed to publish category deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish category deleted event", e);
        }
    }

    public void publishCategoryHierarchyChangedEvent(Category category, UUID previousParentId, Integer previousLevel, String previousFullPath) {
        try {
            CategoryEvents.CategoryHierarchyChangedEvent event = CategoryEvents.CategoryHierarchyChangedEvent.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getName())
                    .previousParentId(previousParentId)
                    .newParentId(category.getParentId())
                    .previousLevel(previousLevel)
                    .newLevel(category.getLevel())
                    .previousFullPath(previousFullPath)
                    .newFullPath(category.getFullPath())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing category hierarchy changed event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_CATEGORY_HIERARCHY_CHANGED, category.getId().toString(), event);
            log.info("Category hierarchy changed event published successfully for category ID: {}", category.getId());
        } catch (Exception e) {
            log.error("Failed to publish category hierarchy changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish category hierarchy changed event", e);
        }
    }

    public void publishCategoryProductAssociationEvent(Category category, Product product, boolean associated) {
        try {
            CategoryEvents.CategoryProductAssociationEvent event = CategoryEvents.CategoryProductAssociationEvent.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getName())
                    .productId(product.getId())
                    .productName(product.getName())
                    .associated(associated)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Publishing category product association event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_CATEGORY_PRODUCT_ASSOCIATION, category.getId().toString(), event);
            log.info("Category product association event published successfully for category ID: {}", category.getId());
        } catch (Exception e) {
            log.error("Failed to publish category product association event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish category product association event", e);
        }
    }

    public void publishCategoryImageUpdatedEvent(Category category, String previousImageUrl) {
        try {
            CategoryEvents.CategoryImageUpdatedEvent event = CategoryEvents.CategoryImageUpdatedEvent.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getName())
                    .previousImageUrl(previousImageUrl)
                    .newImageUrl(category.getImageUrl())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing category image updated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_CATEGORY_IMAGE_UPDATED, category.getId().toString(), event);
            log.info("Category image updated event published successfully for category ID: {}", category.getId());
        } catch (Exception e) {
            log.error("Failed to publish category image updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish category image updated event", e);
        }
    }
}