package com.Ecommerce.Product_Service.Services.Kakfa;

import com.Ecommerce.Product_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Product_Service.Entities.Supplier;
import com.Ecommerce.Product_Service.Events.SupplierEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSupplierCreatedEvent(Supplier supplier) {
        try {
            SupplierEvents.SupplierCreatedEvent event = SupplierEvents.SupplierCreatedEvent.builder()
                    .supplierId(supplier.getId())
                    .name(supplier.getName())
                    .contactInfo(supplier.getContactInfo())
                    .address(supplier.getAddress())
                    .contractDetails(supplier.getContractDetails())
                    .rating(supplier.getRating())
                    .productIds(supplier.getProducts() != null
                            ? supplier.getProducts().stream()
                            .map(product -> product.getId())
                            .collect(Collectors.toList())
                            : null)
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing supplier created event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_SUPPLIER_CREATED, supplier.getId().toString(), event);
            log.info("Supplier created event published successfully for supplier ID: {}", supplier.getId());
        } catch (Exception e) {
            log.error("Failed to publish supplier created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish supplier created event", e);
        }
    }

    public void publishSupplierUpdatedEvent(Supplier supplier) {
        try {
            SupplierEvents.SupplierUpdatedEvent event = SupplierEvents.SupplierUpdatedEvent.builder()
                    .supplierId(supplier.getId())
                    .name(supplier.getName())
                    .contactInfo(supplier.getContactInfo())
                    .address(supplier.getAddress())
                    .contractDetails(supplier.getContractDetails())
                    .rating(supplier.getRating())
                    .productIds(supplier.getProducts() != null
                            ? supplier.getProducts().stream()
                            .map(product -> product.getId())
                            .collect(Collectors.toList())
                            : null)
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing supplier updated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_SUPPLIER_UPDATED, supplier.getId().toString(), event);
            log.info("Supplier updated event published successfully for supplier ID: {}", supplier.getId());
        } catch (Exception e) {
            log.error("Failed to publish supplier updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish supplier updated event", e);
        }
    }

    public void publishSupplierDeletedEvent(Supplier supplier) {
        try {
            SupplierEvents.SupplierDeletedEvent event = SupplierEvents.SupplierDeletedEvent.builder()
                    .supplierId(supplier.getId())
                    .name(supplier.getName())
                    .contactInfo(supplier.getContactInfo())
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing supplier deleted event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_SUPPLIER_DELETED, supplier.getId().toString(), event);
            log.info("Supplier deleted event published successfully for supplier ID: {}", supplier.getId());
        } catch (Exception e) {
            log.error("Failed to publish supplier deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish supplier deleted event", e);
        }
    }

    public void publishSupplierRatingChangedEvent(Supplier supplier, BigDecimal previousRating) {
        try {
            SupplierEvents.SupplierRatingChangedEvent event = SupplierEvents.SupplierRatingChangedEvent.builder()
                    .supplierId(supplier.getId())
                    .name(supplier.getName())
                    .previousRating(previousRating)
                    .newRating(supplier.getRating())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing supplier rating changed event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_SUPPLIER_RATING_CHANGED, supplier.getId().toString(), event);
            log.info("Supplier rating changed event published successfully for supplier ID: {}", supplier.getId());
        } catch (Exception e) {
            log.error("Failed to publish supplier rating changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish supplier rating changed event", e);
        }
    }

    public void publishSupplierContractUpdatedEvent(Supplier supplier, Map<String, Object> previousContractDetails) {
        try {
            SupplierEvents.SupplierContractUpdatedEvent event = SupplierEvents.SupplierContractUpdatedEvent.builder()
                    .supplierId(supplier.getId())
                    .name(supplier.getName())
                    .previousContractDetails(previousContractDetails)
                    .newContractDetails(supplier.getContractDetails())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing supplier contract updated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_SUPPLIER_CONTRACT_UPDATED, supplier.getId().toString(), event);
            log.info("Supplier contract updated event published successfully for supplier ID: {}", supplier.getId());
        } catch (Exception e) {
            log.error("Failed to publish supplier contract updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish supplier contract updated event", e);
        }
    }

    public void publishSupplierProductAssociationEvent(Supplier supplier, UUID productId, String productName, boolean associated) {
        try {
            SupplierEvents.SupplierProductAssociationEvent event = SupplierEvents.SupplierProductAssociationEvent.builder()
                    .supplierId(supplier.getId())
                    .supplierName(supplier.getName())
                    .productId(productId)
                    .productName(productName)
                    .associated(associated)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("Publishing supplier product association event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_SUPPLIER_PRODUCT_ASSOCIATION, supplier.getId().toString(), event);
            log.info("Supplier product association event published successfully for supplier ID: {}", supplier.getId());
        } catch (Exception e) {
            log.error("Failed to publish supplier product association event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish supplier product association event", e);
        }
    }
}