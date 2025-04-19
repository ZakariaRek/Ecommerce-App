package com.Ecommerce.User_Service.Services.Kafka;


import com.Ecommerce.User_Service.Config.KafkaConfig;
import com.Ecommerce.User_Service.Events.UserAddressEvents;
import com.Ecommerce.User_Service.Models.AddressType;
import com.Ecommerce.User_Service.Models.UserAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAddressEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserAddressCreatedEvent(UserAddress address) {
        try {
            UserAddressEvents.UserAddressCreatedEvent event = UserAddressEvents.UserAddressCreatedEvent.builder()
                    .addressId(address.getId())
                    .userId(address.getUserId())
                    .addressType(address.getAddressType())
                    .street(address.getStreet())
                    .city(address.getCity())
                    .state(address.getState())
                    .country(address.getCountry())
                    .zipCode(address.getZipCode())
                    .isDefault(address.isDefault())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user address created event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_ADDRESS_CREATED, address.getId(), event);
            log.info("User address created event published successfully for address ID: {}", address.getId());
        } catch (Exception e) {
            log.error("Failed to publish user address created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user address created event", e);
        }
    }

    public void publishUserAddressUpdatedEvent(UserAddress address) {
        try {
            UserAddressEvents.UserAddressUpdatedEvent event = UserAddressEvents.UserAddressUpdatedEvent.builder()
                    .addressId(address.getId())
                    .userId(address.getUserId())
                    .addressType(address.getAddressType())
                    .street(address.getStreet())
                    .city(address.getCity())
                    .state(address.getState())
                    .country(address.getCountry())
                    .zipCode(address.getZipCode())
                    .isDefault(address.isDefault())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user address updated event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_ADDRESS_UPDATED, address.getId(), event);
            log.info("User address updated event published successfully for address ID: {}", address.getId());
        } catch (Exception e) {
            log.error("Failed to publish user address updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user address updated event", e);
        }
    }

    public void publishUserAddressDeletedEvent(UserAddress address) {
        try {
            UserAddressEvents.UserAddressDeletedEvent event = UserAddressEvents.UserAddressDeletedEvent.builder()
                    .addressId(address.getId())
                    .userId(address.getUserId())
                    .addressType(address.getAddressType())
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user address deleted event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_ADDRESS_DELETED, address.getId(), event);
            log.info("User address deleted event published successfully for address ID: {}", address.getId());
        } catch (Exception e) {
            log.error("Failed to publish user address deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user address deleted event", e);
        }
    }

    public void publishUserDefaultAddressChangedEvent(UserAddress address, String previousDefaultAddressId) {
        try {
            UserAddressEvents.UserDefaultAddressChangedEvent event = UserAddressEvents.UserDefaultAddressChangedEvent.builder()
                    .addressId(address.getId())
                    .userId(address.getUserId())
                    .addressType(address.getAddressType())
                    .previousDefaultAddressId(previousDefaultAddressId)
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user default address changed event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_DEFAULT_ADDRESS_CHANGED, address.getId(), event);
            log.info("User default address changed event published successfully for address ID: {}", address.getId());
        } catch (Exception e) {
            log.error("Failed to publish user default address changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user default address changed event", e);
        }
    }

    public void publishUserAddressTypeChangedEvent(UserAddress address, AddressType previousAddressType) {
        try {
            UserAddressEvents.UserAddressTypeChangedEvent event = UserAddressEvents.UserAddressTypeChangedEvent.builder()
                    .addressId(address.getId())
                    .userId(address.getUserId())
                    .previousAddressType(previousAddressType)
                    .newAddressType(address.getAddressType())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing user address type changed event: {}", event);
            kafkaTemplate.send(KafkaConfig.TOPIC_USER_ADDRESS_TYPE_CHANGED, address.getId(), event);
            log.info("User address type changed event published successfully for address ID: {}", address.getId());
        } catch (Exception e) {
            log.error("Failed to publish user address type changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish user address type changed event", e);
        }
    }
}