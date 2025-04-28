package com.Ecommerce.User_Service.Listeners;

import com.Ecommerce.User_Service.Models.AddressType;
import com.Ecommerce.User_Service.Models.UserAddress;
import com.Ecommerce.User_Service.Services.Kafka.UserAddressEventService;
import com.Ecommerce.User_Service.Services.UserAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class UserAddressEntityListener extends AbstractMongoEventListener<UserAddress> {

    private final ConcurrentHashMap<String, Boolean> previousDefaultMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AddressType> previousAddressTypeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> previousDefaultAddressIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, UserAddress> deletedEntities = new ConcurrentHashMap<>();

    private UserAddressEventService userAddressEventService;
    private UserAddressService userAddressService;

    @Autowired
    public void setUserAddressEventService(UserAddressEventService userAddressEventService) {
        this.userAddressEventService = userAddressEventService;
    }

    @Autowired
    public void setUserAddressService(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<UserAddress> event) {
        UserAddress address = event.getSource();

        if (address.getId() != null) {
            previousDefaultMap.put(address.getId(), address.isDefault());
            previousAddressTypeMap.put(address.getId(), address.getAddressType());

            if (address.isDefault() && !previousDefaultMap.getOrDefault(address.getId(), false)) {
                String previousDefaultId = String.valueOf(userAddressService.getAddressById(address.getUserId()));
                if (previousDefaultId != null && !previousDefaultId.equals(address.getId())) {
                    previousDefaultAddressIds.put(address.getUserId(), previousDefaultId);
                }
            }
        }

        super.onBeforeConvert(event);
    }

    @Override
    public void onBeforeDelete(BeforeDeleteEvent<UserAddress> event) {
        // Store the entity before it's deleted
        Object id  = event.getSource().get("_id");
        UserAddress address = userAddressService.getAddressById(id.toString()).orElse(null);
        if (address != null && address.getId() != null) {
            deletedEntities.put(address.getId(), address);
        }
        super.onBeforeDelete(event);
    }

    @Override
    public void onAfterSave(AfterSaveEvent<UserAddress> event) {
        UserAddress address = event.getSource();

        if (previousDefaultMap.containsKey(address.getId()) ||
                previousAddressTypeMap.containsKey(address.getId())) {
            Boolean previousDefault = previousDefaultMap.get(address.getId());
            if (previousDefault != null && !previousDefault && address.isDefault()) {
                String previousDefaultId = previousDefaultAddressIds.remove(address.getUserId());
                if (previousDefaultId != null) {
                    userAddressEventService.publishUserDefaultAddressChangedEvent(address, previousDefaultId);
                }
            }

            AddressType previousType = previousAddressTypeMap.get(address.getId());
            if (previousType != null && !previousType.equals(address.getAddressType())) {
                userAddressEventService.publishUserAddressTypeChangedEvent(address, previousType);
            }

            userAddressEventService.publishUserAddressUpdatedEvent(address);
        } else {
            userAddressEventService.publishUserAddressCreatedEvent(address);

            if (address.isDefault()) {
                String previousDefaultId = String.valueOf(userAddressService.getDefaultAddress(address.getUserId()));
                if (previousDefaultId != null && !previousDefaultId.equals(address.getId())) {
                    userAddressEventService.publishUserDefaultAddressChangedEvent(address, previousDefaultId);
                }
            }
        }

        super.onAfterSave(event);
    }

    @Override
    public void onAfterDelete(AfterDeleteEvent<UserAddress> event) {
        // Retrieve the deleted entity from our map
        UserAddress address = deletedEntities.remove(event.getDocument().get("_id"));
        if (address != null) {
            userAddressEventService.publishUserAddressDeletedEvent(address);

            // Clean up our maps (though the entity is already gone from the DB)
            previousDefaultMap.remove(address.getId());
            previousAddressTypeMap.remove(address.getId());
        }

        super.onAfterDelete(event);
    }
}