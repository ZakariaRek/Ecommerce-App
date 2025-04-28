package com.Ecommerce.User_Service.Listeners;

import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.UserStatus;
import com.Ecommerce.User_Service.Repositories.UserRepository;
import com.Ecommerce.User_Service.Services.Kafka.UserEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UserEntityListener extends AbstractMongoEventListener<User> {

    private final ConcurrentHashMap<String, UserStatus> previousStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> previousRolesMap = new ConcurrentHashMap<>();

    private UserEventService userEventService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    public void setUserEventService(UserEventService userEventService) {
        this.userEventService = userEventService;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<User> event) {
        User user = event.getSource();

        // This is a new user if the ID is null or not present in our maps
        if (user.getId() != null) {
            // Store the current state before the save happens
            previousStatusMap.put(user.getId(), user.getStatus());

            if (user.getRoles() != null) {
                Set<String> roleNames = user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet());
                previousRolesMap.put(user.getId(), roleNames);
            }
        }

        super.onBeforeConvert(event);
    }

    @Override
    public void onAfterSave(AfterSaveEvent<User> event) {
        User user = event.getSource();

        if (previousStatusMap.containsKey(user.getId())) {
            // This is an update to an existing user

            // Check if status changed
            UserStatus previousStatus = previousStatusMap.get(user.getId());
            if (previousStatus != null && !previousStatus.equals(user.getStatus())) {
                userEventService.publishUserStatusChangedEvent(user, previousStatus);
            }

            // Check if roles changed
            Set<String> previousRoles = previousRolesMap.get(user.getId());
            Set<String> currentRoles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet());

            if (previousRoles != null && !previousRoles.equals(currentRoles)) {
                userEventService.publishUserRoleChangedEvent(user, previousRoles);
            }

            // General update event
            userEventService.publishUserUpdatedEvent(user);
        } else {
            // This is a new user
            userEventService.publishUserCreatedEvent(user);
        }

        super.onAfterSave(event);
    }

    @Override
    public void onAfterDelete(AfterDeleteEvent<User> event) {
        Object id  = event.getSource().get("_id");

        User user = userRepository.findById(id.toString());

        if (user != null) {
            userEventService.publishUserDeletedEvent(user);

            // Clean up our maps
            previousStatusMap.remove(user.getId());
            previousRolesMap.remove(user.getId());
        }

        super.onAfterDelete(event);
    }
}