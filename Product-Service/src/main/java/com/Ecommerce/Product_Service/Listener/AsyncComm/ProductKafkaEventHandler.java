package com.Ecommerce.Product_Service.Listener.AsyncComm;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Payload.Kafka.ProductBatchInfoDTO;
import com.Ecommerce.Product_Service.Payload.Kafka.ProductBatchRequestEventDTO;
import com.Ecommerce.Product_Service.Payload.Kafka.ProductBatchResponseDTO;
import com.Ecommerce.Product_Service.Services.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductKafkaEventHandler {

    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "product.batch.request", groupId = "product-service-group")
    public void handleProductBatchRequest(@Payload ProductBatchRequestEventDTO request) {
        log.info("Received product batch request for {} products with correlationId: {}",
                request.getProductIds().size(), request.getCorrelationId());

        try {
            // Get products by IDs and map to ProductBatchInfoDTO
            List<ProductBatchInfoDTO> products = request.getProductIds().stream()
                    .map(productId -> {
                        try {
                            Optional<Product> product = productService.findProductById(productId);
                            return ProductBatchInfoDTO.builder()
                                    .id(product.get().getId())
                                    .name(product.get().getName())
                                    .price(product.get().getPrice())
                                    .build();
                        } catch (Exception e) {
                            log.warn("Product not found: {}", productId);
                            return null;
                        }
                    })
                    .filter(product -> product != null)
                    .toList();

            ProductBatchResponseDTO response = ProductBatchResponseDTO.builder()
                    .correlationId(request.getCorrelationId())
                    .success(true)
                    .message("Products retrieved successfully")
                    .products(products)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send("product.batch.response", request.getCorrelationId(), response);
            log.info("Sent product batch response for correlationId: {} with {} products",
                    request.getCorrelationId(), products.size());

        } catch (Exception e) {
            log.error("Error processing product batch request for correlationId: {}",
                    request.getCorrelationId(), e);

            ProductBatchResponseDTO errorResponse = ProductBatchResponseDTO.builder()
                    .correlationId(request.getCorrelationId())
                    .success(false)
                    .message(e.getMessage())
                    .products(List.of())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send("product.error", request.getCorrelationId(), errorResponse);
        }
    }
}