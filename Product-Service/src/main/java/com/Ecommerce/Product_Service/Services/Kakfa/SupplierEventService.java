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


}