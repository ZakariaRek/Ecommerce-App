package com.Ecommerce.Notification_Service.Controllers;

import com.Ecommerce.Notification_Service.Models.NotificationPreference;
import com.Ecommerce.Notification_Service.Payload.Request.PreferenceRequest;
import com.Ecommerce.Notification_Service.Services.NotificationPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/preferences")
public class NotificationPreferenceController {
    @Autowired
    private NotificationPreferenceService preferenceService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationPreference>> getUserPreferences(@PathVariable UUID userId) {
        List<NotificationPreference> preferences = preferenceService.getAllPreferencesByUserId(userId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/update")
    public ResponseEntity<NotificationPreference> updatePreference(@RequestBody PreferenceRequest request) {
        NotificationPreference preference = preferenceService.updatePreference(
                request.getUserId(),
                request.getNotificationType(),
                request.getChannel(),
                request.isEnabled()
        );
        return ResponseEntity.ok(preference);
    }
}