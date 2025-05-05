package com.Ecommerce.Order_Service.Config;

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
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Kafka configuration for the Order Service
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:order-service}")
    private String groupId;

    // Order Topics
    public static final String TOPIC_ORDER_CREATED = "order-created";
    public static final String TOPIC_ORDER_UPDATED = "order-updated";
    public static final String TOPIC_ORDER_STATUS_CHANGED = "order-status-changed";
    public static final String TOPIC_ORDER_CANCELED = "order-canceled";
    public static final String TOPIC_ORDER_ITEM_ADDED = "order-item-added";
    public static final String TOPIC_ORDER_ITEM_UPDATED = "order-item-updated";

    // External Topics to Listen
    public static final String TOPIC_PAYMENT_CONFIRMED = "payment-confirmed";
    public static final String TOPIC_SHIPPING_UPDATE = "shipping-update";
    public static final String TOPIC_CART_CHECKED_OUT = "cart-checked-out";
    public static final String TOPIC_PRODUCT_PRICE_CHANGED = "product-price-changed";
    public static final String TOPIC_PRODUCT_STOCK_CHANGED = "product-stock-changed";

    /**
     * Producer configuration
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for sending messages
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Consumer configuration
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // Order Topic definitions
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderUpdatedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_STATUS_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCanceledTopic() {
        return TopicBuilder.name(TOPIC_ORDER_CANCELED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderItemAddedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_ITEM_ADDED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderItemUpdatedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_ITEM_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}