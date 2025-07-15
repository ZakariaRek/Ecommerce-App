package com.Ecommerce.Product_Service.Listener.AsyncComm;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Payload.Kafka.ProductBatchInfoDTO;
import com.Ecommerce.Product_Service.Payload.Kafka.ProductBatchRequestEventDTO;
import com.Ecommerce.Product_Service.Payload.Kafka.ProductBatchResponseDTO;
import com.Ecommerce.Product_Service.Services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductKafkaEventHandler {

    private final ProductService productService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "product.batch.request",
            groupId = "product-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductBatchRequest(ConsumerRecord<String, Object> record) {
        log.info("=== PRODUCT SERVICE KAFKA LISTENER TRIGGERED ===");
        log.info("Received message on topic: {}", record.topic());
        log.info("Message key: {}", record.key());
        log.info("Message value type: {}", record.value().getClass().getName());
        log.info("Message value: {}", record.value());

        try {
            // Parse the request from various formats
            ProductBatchRequestEventDTO request = parseRequest(record.value());

            if (request == null) {
                log.error("Failed to parse request from payload: {}", record.value());
                return;
            }

            log.info("Successfully parsed request - correlationId: {}, productIds count: {}",
                    request.getCorrelationId(), request.getProductIds().size());

            // Process the request
            processProductBatchRequest(request);

        } catch (Exception e) {
            log.error("Error processing product batch request from record: {}", record, e);

            try {
                String correlationId = extractCorrelationId(record.value());
                sendErrorResponse(correlationId, e.getMessage());
            } catch (Exception extractError) {
                log.error("Failed to extract correlationId for error response", extractError);
            }
        }
    }

    /**
     * ✅ Parse request from various payload formats
     */
    private ProductBatchRequestEventDTO parseRequest(Object payload) {
        try {
            if (payload instanceof ProductBatchRequestEventDTO) {
                log.info("Payload is already ProductBatchRequestEventDTO");
                return (ProductBatchRequestEventDTO) payload;
            }

            if (payload instanceof String) {
                log.info("Payload is JSON string, parsing...");
                String jsonString = (String) payload;
                ProductBatchRequestEventDTO request = objectMapper.readValue(jsonString, ProductBatchRequestEventDTO.class);
                log.info("Successfully parsed JSON string to DTO: {}", request);
                return request;
            }

            if (payload instanceof Map) {
                log.info("Payload is Map, converting...");
                Map<String, Object> payloadMap = (Map<String, Object>) payload;

                ProductBatchRequestEventDTO request = ProductBatchRequestEventDTO.builder()
                        .correlationId((String) payloadMap.get("correlationId"))
                        .productIds(convertToUuidList(payloadMap.get("productIds")))
                        .timestamp(getLongValue(payloadMap.get("timestamp")))
                        .build();

                log.info("Successfully converted Map to DTO: {}", request);
                return request;
            }

            // Last resort: convert to JSON then parse
            log.info("Payload is {}, trying to convert via JSON...", payload.getClass().getName());
            String jsonString = objectMapper.writeValueAsString(payload);
            ProductBatchRequestEventDTO request = objectMapper.readValue(jsonString, ProductBatchRequestEventDTO.class);
            log.info("Successfully converted via JSON: {}", request);
            return request;

        } catch (Exception e) {
            log.error("Failed to parse request from payload: {}", payload, e);
            return null;
        }
    }

    /**
     * ✅ Process the product batch request using service batch method
     */
    private void processProductBatchRequest(ProductBatchRequestEventDTO request) {
        try {
            log.info("Processing product batch request for {} products", request.getProductIds().size());
            log.info("Product IDs to fetch: {}", request.getProductIds());

            // Use the service's batch method instead of individual fetching
            List<com.Ecommerce.Product_Service.Payload.Product.ProductBatchResponseDTO> serviceBatchResponse =
                    productService.getBatchProductInfo(request.getProductIds());

            log.info("Successfully fetched {} out of {} requested products",
                    serviceBatchResponse.size(), request.getProductIds().size());

            // Convert service DTOs to Kafka DTOs
            List<ProductBatchInfoDTO> products = serviceBatchResponse.stream()
                    .map(this::convertToKafkaDTO)
                    .toList();

            // Send successful response
            ProductBatchResponseDTO response = ProductBatchResponseDTO.builder()
                    .correlationId(request.getCorrelationId())
                    .success(true)
                    .message(String.format("Successfully retrieved %d products", products.size()))
                    .products(products)
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("Sending success response to 'product.batch.response' with correlationId: {}",
                    request.getCorrelationId());
            log.info("Response payload: {}", response);

            kafkaTemplate.send("product.batch.response", request.getCorrelationId(), response)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send product response for correlationId: {}",
                                    request.getCorrelationId(), ex);
                        } else {
                            log.info("Successfully sent product response for correlationId: {}, metadata: {}",
                                    request.getCorrelationId(), result.getRecordMetadata());
                        }
                    });

        } catch (Exception e) {
            log.error("Error processing product batch request for correlationId: {}",
                    request.getCorrelationId(), e);
            sendErrorResponse(request.getCorrelationId(), e.getMessage());
        }
    }

    /**
     * ✅ Convert service DTO to Kafka DTO
     */
    private ProductBatchInfoDTO convertToKafkaDTO(com.Ecommerce.Product_Service.Payload.Product.ProductBatchResponseDTO serviceDTO) {


        return ProductBatchInfoDTO.builder()
                .id(serviceDTO.getId())
                .name(serviceDTO.getName())
                .price(serviceDTO.getPrice())
                .imagePath(serviceDTO.getImagePath())
                .inStock(serviceDTO.getInStock())
                .availableQuantity(serviceDTO.getAvailableQuantity())
                .status(serviceDTO.getStatus())
                .discountType(serviceDTO.getDiscountType())
                .discountValue(serviceDTO.getDiscountValue())
                .build();
    }
    /**
     * ✅ Send error response
     */
    private void sendErrorResponse(String correlationId, String errorMessage) {
        try {
            ProductBatchResponseDTO errorResponse = ProductBatchResponseDTO.builder()
                    .correlationId(correlationId)
                    .success(false)
                    .message("Error retrieving products: " + errorMessage)
                    .products(List.of())
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("Sending error response to 'product.error' with correlationId: {}", correlationId);

            kafkaTemplate.send("product.error", correlationId, errorResponse)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send error response for correlationId: {}", correlationId, ex);
                        } else {
                            log.info("Successfully sent error response for correlationId: {}", correlationId);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to send error response for correlationId: {}", correlationId, e);
        }
    }

    /**
     * ✅ Helper methods
     */
    private String getImagePath(Product product) {
        try {
            return product.getImages().get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getInStock(Product product) {
        try {
            return product.getStock() > 0;
        } catch (Exception e) {
            return true;
        }
    }

    private Integer getAvailableQuantity(Product product) {
        try {
            return product.getStock();
        } catch (Exception e) {
            return 999; // Default high value
        }
    }

    private String getCategoryName(Product product) {
        try {
            if (product.getCategories() != null && !product.getCategories().isEmpty()) {
                return product.getCategories().get(0).getName();
            }
            return null;
        } catch (Exception e) {
            log.debug("Could not access categories for product {}: {}", product.getId(), e.getMessage());
            return null;
        }
    }
    /**
     * ✅ Convert productIds to UUID list
     */
    private List<UUID> convertToUuidList(Object productIdsObj) {
        if (productIdsObj instanceof List) {
            List<?> productIdsList = (List<?>) productIdsObj;
            return productIdsList.stream()
                    .map(id -> {
                        if (id instanceof String) {
                            return UUID.fromString((String) id);
                        } else if (id instanceof UUID) {
                            return (UUID) id;
                        } else {
                            throw new IllegalArgumentException("Invalid product ID type: " + id.getClass());
                        }
                    })
                    .toList();
        }
        throw new IllegalArgumentException("Product IDs must be a list");
    }

    /**
     * ✅ Get Long value from object
     */
    private Long getLongValue(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return System.currentTimeMillis(); // Default to current time
    }

    /**
     * ✅ Extract correlationId from various payload types
     */
    private String extractCorrelationId(Object payload) {
        try {
            if (payload instanceof ProductBatchRequestEventDTO) {
                return ((ProductBatchRequestEventDTO) payload).getCorrelationId();
            } else if (payload instanceof Map) {
                return (String) ((Map<?, ?>) payload).get("correlationId");
            } else if (payload instanceof String) {
                Map<String, Object> map = objectMapper.readValue((String) payload, Map.class);
                return (String) map.get("correlationId");
            }
        } catch (Exception e) {
            log.warn("Could not extract correlationId from payload: {}", payload, e);
        }
        return "unknown-" + System.currentTimeMillis();
    }
}