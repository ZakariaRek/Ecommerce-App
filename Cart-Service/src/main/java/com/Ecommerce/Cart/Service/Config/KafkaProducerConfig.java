package com.Ecommerce.Cart.Service.Config;

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
 * Comprehensive Kafka configuration for the Cart Service
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:cart-service}")
    private String groupId;

    // Shopping Cart Topics
    public static final String TOPIC_CART_CREATED = "cart-created";
    public static final String TOPIC_CART_UPDATED = "cart-updated";
    public static final String TOPIC_CART_DELETED = "cart-deleted";
    public static final String TOPIC_CART_CHECKED_OUT = "cart-checked-out";
    public static final String TOPIC_CART_ABANDONED = "cart-abandoned";
    public static final String TOPIC_CART_RECOVERED = "cart-recovered";

    // Cart Item Topics
    public static final String TOPIC_CART_ITEM_ADDED = "cart-item-added";
    public static final String TOPIC_CART_ITEM_UPDATED = "cart-item-updated";
    public static final String TOPIC_CART_ITEM_REMOVED = "cart-item-removed";
    public static final String TOPIC_CART_ITEM_QUANTITY_CHANGED = "cart-item-quantity-changed";
    public static final String TOPIC_CART_ITEM_PRICE_CHANGED = "cart-item-price-changed";

    // SavedForLater Topics
    public static final String TOPIC_ITEM_SAVED_FOR_LATER = "item-saved-for-later";
    public static final String TOPIC_SAVED_ITEM_MOVED_TO_CART = "saved-item-moved-to-cart";
    public static final String TOPIC_SAVED_ITEM_REMOVED = "saved-item-removed";

    // Coupon Topics
    public static final String TOPIC_COUPON_APPLIED = "coupon-applied";
    public static final String TOPIC_COUPON_REMOVED = "coupon-removed";
    public static final String TOPIC_COUPON_EXPIRED = "coupon-expired";
    public static final String TOPIC_COUPON_VALIDATED = "coupon-validated";

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

    // Shopping Cart Topic definitions
    @Bean
    public NewTopic cartCreatedTopic() {
        return TopicBuilder.name(TOPIC_CART_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartUpdatedTopic() {
        return TopicBuilder.name(TOPIC_CART_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartDeletedTopic() {
        return TopicBuilder.name(TOPIC_CART_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartCheckedOutTopic() {
        return TopicBuilder.name(TOPIC_CART_CHECKED_OUT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartAbandonedTopic() {
        return TopicBuilder.name(TOPIC_CART_ABANDONED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartRecoveredTopic() {
        return TopicBuilder.name(TOPIC_CART_RECOVERED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Cart Item Topic definitions
    @Bean
    public NewTopic cartItemAddedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_ADDED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartItemUpdatedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartItemRemovedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_REMOVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartItemQuantityChangedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_QUANTITY_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cartItemPriceChangedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_PRICE_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // SavedForLater Topic definitions
    @Bean
    public NewTopic itemSavedForLaterTopic() {
        return TopicBuilder.name(TOPIC_ITEM_SAVED_FOR_LATER)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic savedItemMovedToCartTopic() {
        return TopicBuilder.name(TOPIC_SAVED_ITEM_MOVED_TO_CART)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic savedItemRemovedTopic() {
        return TopicBuilder.name(TOPIC_SAVED_ITEM_REMOVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Coupon Topic definitions
    @Bean
    public NewTopic couponAppliedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_APPLIED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic couponRemovedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_REMOVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic couponExpiredTopic() {
        return TopicBuilder.name(TOPIC_COUPON_EXPIRED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic couponValidatedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_VALIDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}