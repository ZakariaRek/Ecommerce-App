package com.Ecommerce.User_Service.Services.Kafka;

import com.Ecommerce.User_Service.Config.KafkaConfig;
import com.Ecommerce.User_Service.Events.RoleEvents;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRoleCreatedEvent(Role role) {
        try {
            RoleEvents.RoleCreatedEvent event = RoleEvents.RoleCreatedEvent.builder()
                    .roleId(role.getId())
                    .roleName(role.getName())
                    .description(role.getDescription())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing role created event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_ROLE_CREATED, role.getId(), event);
            log.info("Role created event published successfully for role ID: {}", role.getId());
        } catch (Exception e) {
            log.error("Failed to publish role created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish role created event", e);
        }
    }

    public void publishRoleUpdatedEvent(Role role) {
        try {
            RoleEvents.RoleUpdatedEvent event = RoleEvents.RoleUpdatedEvent.builder()
                    .roleId(role.getId())
                    .roleName(role.getName())
                    .description(role.getDescription())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing role updated event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_ROLE_UPDATED, role.getId(), event);
            log.info("Role updated event published successfully for role ID: {}", role.getId());
        } catch (Exception e) {
            log.error("Failed to publish role updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish role updated event", e);
        }
    }

    public void publishRoleDeletedEvent(Role role) {
        try {
            RoleEvents.RoleDeletedEvent event = RoleEvents.RoleDeletedEvent.builder()
                    .roleId(role.getId())
                    .roleName(role.getName())
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing role deleted event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_ROLE_DELETED, role.getId(), event);
            log.info("Role deleted event published successfully for role ID: {}", role.getId());
        } catch (Exception e) {
            log.error("Failed to publish role deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish role deleted event", e);
        }
    }


}