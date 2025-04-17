package com.Ecommerce.Product_Service.Config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Topics
    public static final String TOPIC_PRODUCT_CREATED = "product-created";
    public static final String TOPIC_PRODUCT_UPDATED = "product-updated";
    public static final String TOPIC_PRODUCT_DELETED = "product-deleted";
    public static final String TOPIC_PRODUCT_STOCK_CHANGED = "product-stock-changed";
    public static final String TOPIC_PRODUCT_PRICE_CHANGED = "product-price-changed";
    public static final String TOPIC_PRODUCT_STATUS_CHANGED = "product-status-changed";

    // Producer configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Additional configuration for reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Topic definitions with partitions and replication factor
    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_CREATED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_UPDATED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_DELETED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic productStockChangedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_STOCK_CHANGED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic productPriceChangedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_PRICE_CHANGED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic productStatusChangedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_STATUS_CHANGED)
                .partitions(3)
                .replicas(2)
                .build();
    }
}