package com.Ecommerce.User_Service.Listeners;

import com.Ecommerce.User_Service.Models.ERole;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Services.Kafka.RoleEventService;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class RoleEntityListener extends AbstractMongoEventListener<Role> {

    private RoleEventService roleEventService;

    @Autowired
    public void setRoleEventService(RoleEventService roleEventService) {
        this.roleEventService = roleEventService;
    }

    @Override
    public void onAfterSave(AfterSaveEvent<Role> event) {
        Role role = event.getSource();

        // Determine if this is a new role or an update based on timestamps
        if (role.getCreatedAt().equals(role.getUpdatedAt())) {
            // If creation and update times are the same, it's likely a new role
            roleEventService.publishRoleCreatedEvent(role);
        } else {
            // Otherwise, it's an update
            roleEventService.publishRoleUpdatedEvent(role);
        }

        super.onAfterSave(event);
    }

    @Override
    public void onAfterDelete(AfterDeleteEvent<Role> event) {
        // Handle the case where source might be a Document
        Object source = event.getSource();

        if (source instanceof Role) {
            // If it's already a Role, use it directly
            Role role = (Role) source;
            roleEventService.publishRoleDeletedEvent(role);
        } else if (source instanceof Document) {
            // If it's a Document, extract the necessary fields to create a Role
            Document document = (Document) source;

            try {
                Role role = new Role();
                role.setId(document.getString("_id"));

                // Try to convert the name string to ERole enum
                String roleNameStr = document.getString("name");
                if (roleNameStr != null) {
                    try {
                        ERole roleName = ERole.valueOf(roleNameStr);
                        role.setName(roleName);
                    } catch (IllegalArgumentException e) {
                        log.warn("Could not convert role name string to enum: {}", roleNameStr);
                    }
                }

                role.setDescription(document.getString("description"));

                roleEventService.publishRoleDeletedEvent(role);
            } catch (Exception e) {
                log.error("Error converting Document to Role for delete event: {}", e.getMessage(), e);
            }
        } else {
            log.warn("Could not process delete event: Source was neither Role nor Document");
        }

        super.onAfterDelete(event);
    }
}