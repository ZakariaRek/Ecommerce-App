package com.Ecommerce.Cart.Service.Config;

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

    // Cart Topics
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

    // Coupon Topics
    public static final String TOPIC_COUPON_APPLIED = "coupon-applied";
    public static final String TOPIC_COUPON_REMOVED = "coupon-removed";
    public static final String TOPIC_COUPON_EXPIRED = "coupon-expired";
    public static final String TOPIC_COUPON_VALIDATED = "coupon-validated";

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

    // Cart Topic definitions
    @Bean
    public NewTopic cartCreatedTopic() {
        return TopicBuilder.name(TOPIC_CART_CREATED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartUpdatedTopic() {
        return TopicBuilder.name(TOPIC_CART_UPDATED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartDeletedTopic() {
        return TopicBuilder.name(TOPIC_CART_DELETED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartCheckedOutTopic() {
        return TopicBuilder.name(TOPIC_CART_CHECKED_OUT)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartAbandonedTopic() {
        return TopicBuilder.name(TOPIC_CART_ABANDONED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartRecoveredTopic() {
        return TopicBuilder.name(TOPIC_CART_RECOVERED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    // Cart Item Topic definitions
    @Bean
    public NewTopic cartItemAddedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_ADDED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartItemUpdatedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_UPDATED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartItemRemovedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_REMOVED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartItemQuantityChangedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_QUANTITY_CHANGED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic cartItemPriceChangedTopic() {
        return TopicBuilder.name(TOPIC_CART_ITEM_PRICE_CHANGED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    // Coupon Topic definitions
    @Bean
    public NewTopic couponAppliedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_APPLIED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic couponRemovedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_REMOVED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic couponExpiredTopic() {
        return TopicBuilder.name(TOPIC_COUPON_EXPIRED)
                .partitions(3)
                .replicas(2)
                .build();
    }

    @Bean
    public NewTopic couponValidatedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_VALIDATED)
                .partitions(3)
                .replicas(2)
                .build();
    }
}