package com.Ecommerce.Order_Service.Config;

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

    // Order Request/Response Topics
    public static final String TOPIC_ORDER_REQUEST = "order.request";
    public static final String TOPIC_ORDER_RESPONSE = "order.response";
    public static final String TOPIC_ORDER_ERROR = "order.error";


    public static final String TOPIC_ORDER_BATCH_REQUEST = "order.batch.request";
    public static final String TOPIC_ORDER_BATCH_RESPONSE = "order.batch.response";
    public static final String TOPIC_ORDER_BATCH_ERROR = "order.batch.error";


    public static final String TOPIC_ORDER_COMPLETED = "order-completed";

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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId); // Use the correct group ID
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Fix JsonDeserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        // Remove the specific default type or use a generic Object type
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);

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
    @Bean
    public NewTopic OrderProductStockChanged() {
        return TopicBuilder.name(TOPIC_PRODUCT_STOCK_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderRequestTopic() {
        return TopicBuilder.name(TOPIC_ORDER_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderResponseTopic() {
        return TopicBuilder.name(TOPIC_ORDER_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderErrorTopic() {
        return TopicBuilder.name(TOPIC_ORDER_ERROR)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Add these bean definitions at the end of the configuration class
    @Bean
    public NewTopic orderBatchRequestTopic() {
        return TopicBuilder.name(TOPIC_ORDER_BATCH_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderBatchResponseTopic() {
        return TopicBuilder.name(TOPIC_ORDER_BATCH_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderBatchErrorTopic() {
        return TopicBuilder.name(TOPIC_ORDER_BATCH_ERROR)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCompletedTopic() {
        return TopicBuilder.name(TOPIC_ORDER_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

}