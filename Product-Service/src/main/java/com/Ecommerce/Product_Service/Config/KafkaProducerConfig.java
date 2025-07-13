package com.Ecommerce.Product_Service.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Product Topics
    public static final String TOPIC_PRODUCT_CREATED = "product-created";
    public static final String TOPIC_PRODUCT_UPDATED = "product-updated";
    public static final String TOPIC_PRODUCT_DELETED = "product-deleted";
    public static final String TOPIC_PRODUCT_STOCK_CHANGED = "product-stock-changed";
    public static final String TOPIC_PRODUCT_PRICE_CHANGED = "product-price-changed";
    public static final String TOPIC_PRODUCT_STATUS_CHANGED = "product-status-changed";

    // Supplier Topics
    public static final String TOPIC_SUPPLIER_CREATED = "supplier-created";
    public static final String TOPIC_SUPPLIER_UPDATED = "supplier-updated";
    public static final String TOPIC_SUPPLIER_DELETED = "supplier-deleted";
    public static final String TOPIC_SUPPLIER_RATING_CHANGED = "supplier-rating-changed";
    public static final String TOPIC_SUPPLIER_CONTRACT_UPDATED = "supplier-contract-updated";
    public static final String TOPIC_SUPPLIER_PRODUCT_ASSOCIATION = "supplier-product-association";

    // Inventory Topics
    public static final String TOPIC_INVENTORY_CREATED = "inventory-created";
    public static final String TOPIC_INVENTORY_UPDATED = "inventory-updated";
    public static final String TOPIC_INVENTORY_DELETED = "inventory-deleted";
    public static final String TOPIC_INVENTORY_STOCK_CHANGED = "inventory-stock-changed";
    public static final String TOPIC_INVENTORY_THRESHOLD_CHANGED = "inventory-threshold-changed";
    public static final String TOPIC_INVENTORY_LOW_STOCK = "inventory-low-stock";
    public static final String TOPIC_INVENTORY_RESTOCKED = "inventory-restocked";

    public static final String TOPIC_CATEGORY_CREATED = "category-created";
    public static final String TOPIC_CATEGORY_UPDATED = "category-updated";
    public static final String TOPIC_CATEGORY_DELETED = "category-deleted";
    public static final String TOPIC_CATEGORY_HIERARCHY_CHANGED = "category-hierarchy-changed";
    public static final String TOPIC_CATEGORY_PRODUCT_ASSOCIATION = "category-product-association";
    public static final String TOPIC_CATEGORY_IMAGE_UPDATED = "category-image-updated";

    public static final String TOPIC_DISCOUNT_CREATED = "discount-created";
    public static final String TOPIC_DISCOUNT_UPDATED = "discount-updated";
    public static final String TOPIC_DISCOUNT_DELETED = "discount-deleted";
    public static final String TOPIC_DISCOUNT_ACTIVATED = "discount-activated";
    public static final String TOPIC_DISCOUNT_DEACTIVATED = "discount-deactivated";
    public static final String TOPIC_DISCOUNT_VALUE_CHANGED = "discount-value-changed";
    public static final String TOPIC_DISCOUNT_PERIOD_CHANGED = "discount-period-changed";


    // Review Topics
    public static final String TOPIC_REVIEW_CREATED = "review-created";
    public static final String TOPIC_REVIEW_UPDATED = "review-updated";
    public static final String TOPIC_REVIEW_DELETED = "review-deleted";
    public static final String TOPIC_REVIEW_VERIFIED = "review-verified";
    public static final String TOPIC_REVIEW_RATING_CHANGED = "review-rating-changed";

    // Producer configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Add additional producer properties for reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "product-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // ✅ CRITICAL: Configure JsonDeserializer properly
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); // Allow all packages for now
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false); // Don't rely on type headers
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.Ecommerce.Product_Service.Payload.Kafka.ProductBatchRequestEventDTO");

        // Consumer reliability settings
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Configure container properties
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setPollTimeout(3000);

        return factory;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Register JavaTime module for LocalDateTime
        return mapper;
    }

    // Product Topic definitions with partitions and replication factor
    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productStockChangedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_STOCK_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productPriceChangedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_PRICE_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productStatusChangedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_STATUS_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Supplier Topic definitions with partitions and replication factor
    @Bean
    public NewTopic supplierCreatedTopic() {
        return TopicBuilder.name(TOPIC_SUPPLIER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic supplierUpdatedTopic() {
        return TopicBuilder.name(TOPIC_SUPPLIER_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic supplierDeletedTopic() {
        return TopicBuilder.name(TOPIC_SUPPLIER_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic supplierRatingChangedTopic() {
        return TopicBuilder.name(TOPIC_SUPPLIER_RATING_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic supplierContractUpdatedTopic() {
        return TopicBuilder.name(TOPIC_SUPPLIER_CONTRACT_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic supplierProductAssociationTopic() {
        return TopicBuilder.name(TOPIC_SUPPLIER_PRODUCT_ASSOCIATION)
                .partitions(3)
                .replicas(1)
                .build();
    }
    // Category Topic definitions with partitions and replication factor
    @Bean
    public NewTopic categoryCreatedTopic() {
        return TopicBuilder.name(TOPIC_CATEGORY_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic categoryUpdatedTopic() {
        return TopicBuilder.name(TOPIC_CATEGORY_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic categoryDeletedTopic() {
        return TopicBuilder.name(TOPIC_CATEGORY_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic categoryHierarchyChangedTopic() {
        return TopicBuilder.name(TOPIC_CATEGORY_HIERARCHY_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic categoryProductAssociationTopic() {
        return TopicBuilder.name(TOPIC_CATEGORY_PRODUCT_ASSOCIATION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic categoryImageUpdatedTopic() {
        return TopicBuilder.name(TOPIC_CATEGORY_IMAGE_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Discount Topic definitions with partitions and replication factor
    @Bean
    public NewTopic discountCreatedTopic() {
        return TopicBuilder.name(TOPIC_DISCOUNT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic discountUpdatedTopic() {
        return TopicBuilder.name(TOPIC_DISCOUNT_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic discountDeletedTopic() {
        return TopicBuilder.name(TOPIC_DISCOUNT_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic discountActivatedTopic() {
        return TopicBuilder.name(TOPIC_DISCOUNT_ACTIVATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic discountDeactivatedTopic() {
        return TopicBuilder.name(TOPIC_DISCOUNT_DEACTIVATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic discountValueChangedTopic() {
        return TopicBuilder.name(TOPIC_DISCOUNT_VALUE_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic discountPeriodChangedTopic() {
        return TopicBuilder.name(TOPIC_DISCOUNT_PERIOD_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Review Topic definitions with partitions and replication factor
    @Bean
    public NewTopic reviewCreatedTopic() {
        return TopicBuilder.name(TOPIC_REVIEW_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reviewUpdatedTopic() {
        return TopicBuilder.name(TOPIC_REVIEW_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reviewDeletedTopic() {
        return TopicBuilder.name(TOPIC_REVIEW_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reviewVerifiedTopic() {
        return TopicBuilder.name(TOPIC_REVIEW_VERIFIED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic reviewRatingChangedTopic() {
        return TopicBuilder.name(TOPIC_REVIEW_RATING_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }
    // Inventory Topic definitions with partitions and replication factor
    @Bean
    public NewTopic inventoryCreatedTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryUpdatedTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryDeletedTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryStockChangedTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_STOCK_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryThresholdChangedTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_THRESHOLD_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryLowStockTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_LOW_STOCK)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryRestockedTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_RESTOCKED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}