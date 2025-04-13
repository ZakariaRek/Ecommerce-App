package com.Ecommerce.User_Service.Config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${User-service.kafka.topics.user-created}")
    private String userCreatedTopic;

    @Value("${User-service.kafka.topics.user-updated}")
    private String userUpdatedTopic;

    @Value("${User-service.kafka.topics.user-deleted}")
    private String userDeletedTopic;

    @Value("${User-service.kafka.topics.user-status-changed}")
    private String userStatusChangedTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userCreatedTopic() {
        return TopicBuilder.name(userCreatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return TopicBuilder.name(userUpdatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDeletedTopic() {
        return TopicBuilder.name(userDeletedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userStatusChangedTopic() {
        return TopicBuilder.name(userStatusChangedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}