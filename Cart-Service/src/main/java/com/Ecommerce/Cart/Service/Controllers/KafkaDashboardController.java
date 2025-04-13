package com.Ecommerce.Cart.Service.Controllers;

import com.Ecommerce.Cart.Service.Payload.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/kafka")
@RequiredArgsConstructor
public class KafkaDashboardController {

    private final KafkaAdmin kafkaAdmin;
    private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    /**
     * Get Kafka cluster and service information
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKafkaInfo() throws ExecutionException, InterruptedException {
        Map<String, Object> kafkaInfo = new HashMap<>();

        // Get topics information
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // Get topics
            ListTopicsResult topics = adminClient.listTopics();
            Collection<TopicListing> topicListings = topics.listings().get();
            Set<String> topicNames = topicListings.stream()
                    .map(TopicListing::name)
                    .collect(Collectors.toSet());
            kafkaInfo.put("topics", topicNames);

            // Get consumer groups
            ListConsumerGroupsResult consumerGroups = adminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> groupListings = consumerGroups.all().get();
            Set<String> groupIds = groupListings.stream()
                    .map(ConsumerGroupListing::groupId)
                    .collect(Collectors.toSet());
            kafkaInfo.put("consumerGroups", groupIds);
        }

        // Get listener information
        Map<String, Object> listenerInfo = new HashMap<>();
        kafkaListenerEndpointRegistry.getListenerContainerIds().forEach(id -> {
            Map<String, Object> containerInfo = new HashMap<>();
            containerInfo.put("isRunning", kafkaListenerEndpointRegistry.getListenerContainer(id).isRunning());
            containerInfo.put("groupId", kafkaListenerEndpointRegistry.getListenerContainer(id).getGroupId());
            containerInfo.put("listenerId", id);

            listenerInfo.put(id, containerInfo);
        });
        kafkaInfo.put("activeListeners", listenerInfo);

        return ResponseEntity.ok(ApiResponse.success(kafkaInfo));
    }

    /**
     * Get status of Kafka listeners in this service
     */
    @GetMapping("/listeners")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getListenersStatus() {
        Map<String, Object> listenersStatus = new HashMap<>();

        kafkaListenerEndpointRegistry.getListenerContainerIds().forEach(id -> {
            Map<String, Object> containerInfo = new HashMap<>();
            containerInfo.put("isRunning", kafkaListenerEndpointRegistry.getListenerContainer(id).isRunning());
            containerInfo.put("isPaused", kafkaListenerEndpointRegistry.getListenerContainer(id).isPauseRequested());
            containerInfo.put("groupId", kafkaListenerEndpointRegistry.getListenerContainer(id).getGroupId());

            listenersStatus.put(id, containerInfo);
        });

        return ResponseEntity.ok(ApiResponse.success(listenersStatus));
    }
}