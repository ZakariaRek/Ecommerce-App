package com.Ecommerce.Notification_Service.Services;


import com.Ecommerce.Notification_Service.Listeners.NotificationPreferenceMongoListener;
import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import com.Ecommerce.Notification_Service.Models.NotificationPreference;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Repositories.NotificationPreferenceRepository;
import com.Ecommerce.Notification_Service.Services.Kafka.NotificationPreferenceKafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationPreferenceService {
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private NotificationPreferenceKafkaService kafkaService;

    @Autowired
    private NotificationPreferenceMongoListener preferenceMongoListener;

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

            // Store the preference state before saving for event tracking
            preferenceMongoListener.storeStateBeforeSave(preference);

            preference.setEnabled(isEnabled);
        } else {
            preference = new NotificationPreference(userId, type, channel, isEnabled);
            // No need to store state as it's a new preference
        }

        return preferenceRepository.save(preference);
    }

    /**
     * Set default preferences for a new user
     */
    @Transactional
    public void setDefaultPreferences(UUID userId) {
        // Create default preferences for all notification types and channels
        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                // By default, enable in-app notifications for all types
                // and email for important notifications (order status, payment)
                boolean enabled = channel == NotificationChannel.IN_APP ||
                        (channel == NotificationChannel.EMAIL &&
                                (type == NotificationType.ORDER_STATUS ||
                                        type == NotificationType.PAYMENT_CONFIRMATION ||
                                        type == NotificationType.SHIPPING_UPDATE));

                updatePreference(userId, type, channel, enabled);
            }
        }
    }

    /**
     * Opt user out of all notifications across all channels
     */
    @Transactional
    public void optOutAll(UUID userId, String reason) {
        List<NotificationPreference> userPreferences = getAllPreferencesByUserId(userId);

        for (NotificationPreference preference : userPreferences) {
            if (preference.isEnabled()) {
                // Store the preference state before saving for event tracking
                preferenceMongoListener.storeStateBeforeSave(preference);

                preference.setEnabled(false);
                preferenceRepository.save(preference);
            }
        }

        // Publish a user opted out event
        kafkaService.publishUserOptedOut(userId, reason);
    }
}