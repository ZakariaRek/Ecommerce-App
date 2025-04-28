package com.Ecommerce.Loyalty_Service.config;

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

/**
 * Comprehensive Kafka configuration for the Loyalty Service
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:loyalty-service}")
    private String groupId;

    // Loyalty Point Topics
    public static final String TOPIC_POINTS_EARNED = "loyalty-points-earned";
    public static final String TOPIC_POINTS_REDEEMED = "loyalty-points-redeemed";
    public static final String TOPIC_POINTS_EXPIRED = "loyalty-points-expired";
    public static final String TOPIC_POINTS_ADJUSTED = "loyalty-points-adjusted";

    // Membership Topics
    public static final String TOPIC_MEMBERSHIP_CHANGED = "loyalty-membership-changed";
    public static final String TOPIC_MEMBERSHIP_BENEFITS_UPDATED = "loyalty-membership-benefits-updated";

    // Coupon Topics
    public static final String TOPIC_COUPON_GENERATED = "loyalty-coupon-generated";
    public static final String TOPIC_COUPON_REDEEMED = "loyalty-coupon-redeemed";
    public static final String TOPIC_COUPON_EXPIRED = "loyalty-coupon-expired";
    public static final String TOPIC_COUPON_VALIDATED = "loyalty-coupon-validated";

    // Reward Topics
    public static final String TOPIC_REWARD_ADDED = "loyalty-reward-added";
    public static final String TOPIC_REWARD_REDEEMED = "loyalty-reward-redeemed";
    public static final String TOPIC_REWARD_UPDATED = "loyalty-reward-updated";

    // External Topics (consumed by Loyalty Service)
    public static final String TOPIC_ORDER_COMPLETED = "order-completed";
    public static final String TOPIC_USER_REGISTERED = "user-registered";
    public static final String TOPIC_PRODUCT_REVIEWED = "product-reviewed";
    public static final String TOPIC_USER_PROFILE_UPDATED = "user-profile-updated";
    public static final String TOPIC_CART_ABANDONED = "cart-abandoned";
    public static final String TOPIC_USER_REFERRAL_COMPLETED = "user-referral-completed";

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
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.Ecommerce.*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // Points Topics
    @Bean
    public NewTopic pointsEarnedTopic() {
        return TopicBuilder.name(TOPIC_POINTS_EARNED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pointsRedeemedTopic() {
        return TopicBuilder.name(TOPIC_POINTS_REDEEMED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pointsExpiredTopic() {
        return TopicBuilder.name(TOPIC_POINTS_EXPIRED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic pointsAdjustedTopic() {
        return TopicBuilder.name(TOPIC_POINTS_ADJUSTED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    // Membership Topics
    @Bean
    public NewTopic membershipChangedTopic() {
        return TopicBuilder.name(TOPIC_MEMBERSHIP_CHANGED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic membershipBenefitsUpdatedTopic() {
        return TopicBuilder.name(TOPIC_MEMBERSHIP_BENEFITS_UPDATED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    // Coupon Topics
    @Bean
    public NewTopic couponGeneratedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_GENERATED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic couponRedeemedTopic() {
        return TopicBuilder.name(TOPIC_COUPON_REDEEMED)
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

    // Reward Topics
    @Bean
    public NewTopic rewardAddedTopic() {
        return TopicBuilder.name(TOPIC_REWARD_ADDED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rewardRedeemedTopic() {
        return TopicBuilder.name(TOPIC_REWARD_REDEEMED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rewardUpdatedTopic() {
        return TopicBuilder.name(TOPIC_REWARD_UPDATED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    // External Topics (consumed) - Define them for auto-creation if they don't exist
    @Bean
    public NewTopic cartAbandonedTopic() {
        return TopicBuilder.name(TOPIC_CART_ABANDONED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productReviewedTopic() {
        return TopicBuilder.name(TOPIC_PRODUCT_REVIEWED)
                .partitions(3)
                 .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userReferralCompletedTopic() {
        return TopicBuilder.name(TOPIC_USER_REFERRAL_COMPLETED)
                .partitions(3)
                 .replicas(1)
                .build();
    }
}