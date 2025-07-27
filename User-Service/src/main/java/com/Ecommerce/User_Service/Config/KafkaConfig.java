package com.Ecommerce.User_Service.Config;

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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // User Topics
    public static final String TOPIC_USER_CREATED = "user-created";
    public static final String TOPIC_USER_UPDATED = "user-updated";
    public static final String TOPIC_USER_DELETED = "user-deleted";
    public static final String TOPIC_USER_STATUS_CHANGED = "user-status-changed";
    public static final String TOPIC_USER_ROLE_CHANGED = "user-role-changed";

    // User Address Topics
    public static final String TOPIC_USER_ADDRESS_CREATED = "user-address-created";
    public static final String TOPIC_USER_ADDRESS_UPDATED = "user-address-updated";
    public static final String TOPIC_USER_ADDRESS_DELETED = "user-address-deleted";
    public static final String TOPIC_USER_DEFAULT_ADDRESS_CHANGED = "user-default-address-changed";
    public static final String TOPIC_USER_ADDRESS_TYPE_CHANGED = "user-address-type-changed";

    // Role Topics
    public static final String TOPIC_ROLE_CREATED = "role-created";
    public static final String TOPIC_ROLE_UPDATED = "role-updated";
    public static final String TOPIC_ROLE_DELETED = "role-deleted";
    public static final String TOPIC_ROLE_ASSIGNED_TO_USER = "role-assigned-to-user";
    public static final String TOPIC_ROLE_REMOVED_FROM_USER = "role-removed-from-user";

    public static final String TOPIC_USER_EMAIL_REQUEST = "user-email-request";
    public static final String TOPIC_USER_EMAIL_RESPONSE = "user-email-response";
    public static final String TOPIC_BULK_USER_EMAIL_REQUEST = "bulk-user-email-request";
    public static final String TOPIC_BULK_USER_EMAIL_RESPONSE = "bulk-user-email-response";
    public static final String TOPIC_USER_INFO_REQUEST = "user-info-request";
    public static final String TOPIC_USER_INFO_RESPONSE = "user-info-response";
    public static final String TOPIC_BULK_USER_INFO_REQUEST = "bulk-user-info-request";
    public static final String TOPIC_BULK_USER_INFO_RESPONSE = "bulk-user-info-response";
    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Additional reliability configuration
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.Ecommerce.*");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // Topic definitions - User topics
    @Bean
    public NewTopic userCreatedTopic() {
        return TopicBuilder.name(TOPIC_USER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return TopicBuilder.name(TOPIC_USER_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return TopicBuilder.name(TOPIC_USER_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userStatusChangedTopic() {
        return TopicBuilder.name(TOPIC_USER_STATUS_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRoleChangedTopic() {
        return TopicBuilder.name(TOPIC_USER_ROLE_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Topic definitions - User Address topics
    @Bean
    public NewTopic userAddressCreatedTopic() {
        return TopicBuilder.name(TOPIC_USER_ADDRESS_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userAddressUpdatedTopic() {
        return TopicBuilder.name(TOPIC_USER_ADDRESS_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userAddressDeletedTopic() {
        return TopicBuilder.name(TOPIC_USER_ADDRESS_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDefaultAddressChangedTopic() {
        return TopicBuilder.name(TOPIC_USER_DEFAULT_ADDRESS_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userAddressTypeChangedTopic() {
        return TopicBuilder.name(TOPIC_USER_ADDRESS_TYPE_CHANGED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Topic definitions - Role topics
    @Bean
    public NewTopic roleCreatedTopic() {
        return TopicBuilder.name(TOPIC_ROLE_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic roleUpdatedTopic() {
        return TopicBuilder.name(TOPIC_ROLE_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic roleDeletedTopic() {
        return TopicBuilder.name(TOPIC_ROLE_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic roleAssignedToUserTopic() {
        return TopicBuilder.name(TOPIC_ROLE_ASSIGNED_TO_USER)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic roleRemovedFromUserTopic() {
        return TopicBuilder.name(TOPIC_ROLE_REMOVED_FROM_USER)
                .partitions(3)
                .replicas(1)
                .build();
    }
    // Topic definitions - User Information Request/Response topics
    @Bean
    public NewTopic userEmailRequestTopic() {
        return TopicBuilder.name(TOPIC_USER_EMAIL_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userEmailResponseTopic() {
        return TopicBuilder.name(TOPIC_USER_EMAIL_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bulkUserEmailRequestTopic() {
        return TopicBuilder.name(TOPIC_BULK_USER_EMAIL_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bulkUserEmailResponseTopic() {
        return TopicBuilder.name(TOPIC_BULK_USER_EMAIL_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userInfoRequestTopic() {
        return TopicBuilder.name(TOPIC_USER_INFO_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userInfoResponseTopic() {
        return TopicBuilder.name(TOPIC_USER_INFO_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bulkUserInfoRequestTopic() {
        return TopicBuilder.name(TOPIC_BULK_USER_INFO_REQUEST)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bulkUserInfoResponseTopic() {
        return TopicBuilder.name(TOPIC_BULK_USER_INFO_RESPONSE)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
//package com.Ecommerce.User_Service.Config;
//
//import org.apache.kafka.clients.admin.NewTopic;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.config.TopicBuilder;
//import org.springframework.kafka.core.*;
//        import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class KafkaConfig {
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;
//
//    @Value("${spring.kafka.consumer.group-id}")
//    private String groupId;
//
//    // User Topics
//    public static final String TOPIC_USER_CREATED = "user-created";
//    public static final String TOPIC_USER_UPDATED = "user-updated";
//    public static final String TOPIC_USER_DELETED = "user-deleted";
//    public static final String TOPIC_USER_STATUS_CHANGED = "user-status-changed";
//    public static final String TOPIC_USER_ROLE_CHANGED = "user-role-changed";
//
//    // User Address Topics
//    public static final String TOPIC_USER_ADDRESS_CREATED = "user-address-created";
//    public static final String TOPIC_USER_ADDRESS_UPDATED = "user-address-updated";
//    public static final String TOPIC_USER_ADDRESS_DELETED = "user-address-deleted";
//    public static final String TOPIC_USER_DEFAULT_ADDRESS_CHANGED = "user-default-address-changed";
//    public static final String TOPIC_USER_ADDRESS_TYPE_CHANGED = "user-address-type-changed";
//
//    // Role Topics
//    public static final String TOPIC_ROLE_CREATED = "role-created";
//    public static final String TOPIC_ROLE_UPDATED = "role-updated";
//    public static final String TOPIC_ROLE_DELETED = "role-deleted";
//    public static final String TOPIC_ROLE_ASSIGNED_TO_USER = "role-assigned-to-user";
//    public static final String TOPIC_ROLE_REMOVED_FROM_USER = "role-removed-from-user";
//
//    // User Information Request/Response Topics (for Notification Service communication)
//
//
//    // Producer Configuration
//    @Bean
//    public ProducerFactory<String, Object> producerFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        // Additional reliability configuration
//        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
//        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
//        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
//        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
//        return new DefaultKafkaProducerFactory<>(configProps);
//    }
//
//    @Bean
//    public KafkaTemplate<String, Object> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//
//    // Consumer Configuration
//    @Bean
//    public ConsumerFactory<String, Object> consumerFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.Ecommerce.*");
//        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//        return new DefaultKafkaConsumerFactory<>(configProps);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory());
//        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//        return factory;
//    }
//
//    // Topic definitions - User topics
//    @Bean
//    public NewTopic userCreatedTopic() {
//        return TopicBuilder.name(TOPIC_USER_CREATED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userUpdatedTopic() {
//        return TopicBuilder.name(TOPIC_USER_UPDATED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userDeletedTopic() {
//        return TopicBuilder.name(TOPIC_USER_DELETED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userStatusChangedTopic() {
//        return TopicBuilder.name(TOPIC_USER_STATUS_CHANGED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userRoleChangedTopic() {
//        return TopicBuilder.name(TOPIC_USER_ROLE_CHANGED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    // Topic definitions - User Address topics
//    @Bean
//    public NewTopic userAddressCreatedTopic() {
//        return TopicBuilder.name(TOPIC_USER_ADDRESS_CREATED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userAddressUpdatedTopic() {
//        return TopicBuilder.name(TOPIC_USER_ADDRESS_UPDATED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userAddressDeletedTopic() {
//        return TopicBuilder.name(TOPIC_USER_ADDRESS_DELETED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userDefaultAddressChangedTopic() {
//        return TopicBuilder.name(TOPIC_USER_DEFAULT_ADDRESS_CHANGED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic userAddressTypeChangedTopic() {
//        return TopicBuilder.name(TOPIC_USER_ADDRESS_TYPE_CHANGED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    // Topic definitions - Role topics
//    @Bean
//    public NewTopic roleCreatedTopic() {
//        return TopicBuilder.name(TOPIC_ROLE_CREATED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic roleUpdatedTopic() {
//        return TopicBuilder.name(TOPIC_ROLE_UPDATED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic roleDeletedTopic() {
//        return TopicBuilder.name(TOPIC_ROLE_DELETED)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic roleAssignedToUserTopic() {
//        return TopicBuilder.name(TOPIC_ROLE_ASSIGNED_TO_USER)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }
//
//    @Bean
//    public NewTopic roleRemovedFromUserTopic() {
//        return TopicBuilder.name(TOPIC_ROLE_REMOVED_FROM_USER)
//                .partitions(3)
//                .replicas(1)
//                .build();
//    }


//}