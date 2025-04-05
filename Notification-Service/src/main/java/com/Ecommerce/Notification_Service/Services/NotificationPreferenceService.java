package com.Ecommerce.Notification_Service.Services;


import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import com.Ecommerce.Notification_Service.Models.NotificationPreference;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Repositories.NotificationPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationPreferenceService {
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    public List<NotificationPreference> getAllPreferencesByUserId(UUID userId) {
        return preferenceRepository.findByUserId(userId);
    }

    public NotificationPreference getPreference(UUID userId, NotificationType type, NotificationChannel channel) {
        Optional<NotificationPreference> preference = preferenceRepository.findByUserIdAndNotificationTypeAndChannel(userId, type, channel);
        return preference.orElse(null);
    }

    public boolean isChannelEnabled(UUID userId, NotificationType type, NotificationChannel channel) {
        NotificationPreference preference = getPreference(userId, type, channel);
        return preference != null && preference.isEnabled();
    }

    public NotificationPreference updatePreference(UUID userId, NotificationType type, NotificationChannel channel, boolean isEnabled) {
        Optional<NotificationPreference> existingPreference = preferenceRepository.findByUserIdAndNotificationTypeAndChannel(userId, type, channel);

        NotificationPreference preference;
        if (existingPreference.isPresent()) {
            preference = existingPreference.get();
            preference.setEnabled(isEnabled);
        } else {
            preference = new NotificationPreference(userId, type, channel, isEnabled);
        }

        return preferenceRepository.save(preference);
    }
}