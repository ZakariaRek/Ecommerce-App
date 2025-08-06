package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Listeners.NotificationMongoListener;
import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationChannel;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Repositories.NotificationRepository;
import com.Ecommerce.Notification_Service.Services.Kafka.NotificationKafkaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private NotificationSenderService senderService;

    @Mock
    private NotificationKafkaService kafkaService;

    @Mock
    private NotificationMongoListener notificationMongoListener;

    @Mock
    private SSENotificationService sseNotificationService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID testUserId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testNotification = new Notification(
                testUserId,
                NotificationType.ORDER_STATUS,
                "Test notification content",
                LocalDateTime.now().plusDays(1)
        );
        testNotification.setId("test-notification-id");
    }

    @Test
    void createNotification_ShouldCreateAndSaveNotification() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.createNotification(
                testUserId,
                NotificationType.ORDER_STATUS,
                "Test notification content",
                LocalDateTime.now().plusDays(1)
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getType()).isEqualTo(NotificationType.ORDER_STATUS);
        assertThat(result.getContent()).isEqualTo("Test notification content");
        assertThat(result.isRead()).isFalse();

        verify(notificationRepository).save(any(Notification.class));
        verify(sseNotificationService).sendNotificationToUser(eq(testUserId), any(Notification.class));
    }

    @Test
    void getAllNotificationsByUserId_ShouldReturnUserNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserId(testUserId)).thenReturn(notifications);

        // When
        List<Notification> result = notificationService.getAllNotificationsByUserId(testUserId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testNotification);
        verify(notificationRepository).findByUserId(testUserId);
    }

    @Test
    void getUnreadNotificationsByUserId_ShouldReturnUnreadNotifications() {
        // Given
        List<Notification> unreadNotifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdAndIsRead(testUserId, false)).thenReturn(unreadNotifications);

        // When
        List<Notification> result = notificationService.getUnreadNotificationsByUserId(testUserId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testNotification);
        verify(notificationRepository).findByUserIdAndIsRead(testUserId, false);
    }

    @Test
    void markAsRead_ShouldUpdateNotificationAndSendSSE() {
        // Given
        String notificationId = "test-notification-id";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.markAsRead(notificationId);

        // Then
        assertThat(result).isNotNull();
        verify(notificationMongoListener).storeStateBeforeSave(testNotification);
        verify(notificationRepository).save(testNotification);
        verify(sseNotificationService).sendNotificationToUser(eq(testUserId), any(Notification.class));
    }

    @Test
    void markAsRead_WithNonExistentId_ShouldReturnNull() {
        // Given
        String notificationId = "non-existent-id";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // When
        Notification result = notificationService.markAsRead(notificationId);

        // Then
        assertThat(result).isNull();
        verify(notificationRepository, never()).save(any());
        verify(sseNotificationService, never()).sendNotificationToUser(any(), any());
    }

    @Test
    void deleteNotification_ShouldStoreBeforeDeleteAndDelete() {
        // Given
        String notificationId = "test-notification-id";
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

        // When
        notificationService.deleteNotification(notificationId);

        // Then
        verify(notificationMongoListener).storeBeforeDelete(testNotification);
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    void sendNotification_ShouldCreateNotificationAndSendThroughChannels() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(preferenceService.isChannelEnabled(testUserId, NotificationType.ORDER_STATUS, NotificationChannel.EMAIL))
                .thenReturn(true);
        when(preferenceService.isChannelEnabled(testUserId, NotificationType.ORDER_STATUS, NotificationChannel.IN_APP))
                .thenReturn(true);
        when(preferenceService.isChannelEnabled(testUserId, NotificationType.ORDER_STATUS, NotificationChannel.SMS))
                .thenReturn(false);

        // When
        Notification result = notificationService.sendNotification(
                testUserId,
                NotificationType.ORDER_STATUS,
                "Test notification content",
                LocalDateTime.now().plusDays(1)
        );

        // Then
        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
        verify(sseNotificationService).sendNotificationToUser(eq(testUserId), any(Notification.class));
        verify(senderService).send(any(Notification.class), eq(NotificationChannel.EMAIL));
        verify(senderService).send(any(Notification.class), eq(NotificationChannel.IN_APP));
        verify(senderService, never()).send(any(Notification.class), eq(NotificationChannel.SMS));
    }

    @Test
    void sendBulkNotifications_ShouldCreateNotificationsForAllUsers() {
        // Given
        List<UUID> userIds = Arrays.asList(testUserId, UUID.randomUUID());
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        when(preferenceService.isChannelEnabled(any(UUID.class), any(NotificationType.class), any(NotificationChannel.class)))
                .thenReturn(true);

        // When
        notificationService.sendBulkNotifications(
                userIds,
                NotificationType.PROMOTION,
                "Bulk notification content",
                LocalDateTime.now().plusDays(1)
        );

        // Then
        verify(notificationRepository, times(userIds.size())).save(any(Notification.class));
        verify(sseNotificationService, times(userIds.size())).sendNotificationToUser(any(UUID.class), any(Notification.class));
        verify(kafkaService).publishBulkNotificationSent(
                eq(NotificationType.PROMOTION),
                eq("Bulk notification content"),
                eq(userIds.size())
        );
    }

    @Test
    void broadcastSystemNotification_ShouldSendSystemAlert() {
        // Given
        String title = "System Maintenance";
        String message = "System will be down for maintenance";

        // When
        notificationService.broadcastSystemNotification(title, message, NotificationType.SYSTEM_ALERT);

        // Then
        verify(sseNotificationService).sendSystemAlert(title, message, "SYSTEM_ALERT");
    }

    @Test
    void createProductNotification_ShouldCreateNotificationWithProductContext() {
        // Given
        String productId = "product-123";
        String productName = "Test Product";
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.createProductNotification(
                testUserId,
                NotificationType.PRODUCT_CREATED,
                "New product available",
                productId,
                productName,
                LocalDateTime.now().plusDays(1)
        );

        // Then
        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
        verify(sseNotificationService).sendNotificationToUser(eq(testUserId), any(Notification.class));
    }

    @Test
    void createInventoryNotification_ShouldCreateNotificationWithInventoryContext() {
        // Given
        String productName = "Test Product";
        Integer currentStock = 5;
        Integer threshold = 10;
        String warehouseLocation = "Warehouse A";
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.createInventoryNotification(
                testUserId,
                NotificationType.INVENTORY_LOW_STOCK,
                productName,
                currentStock,
                threshold,
                warehouseLocation
        );

        // Then
        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
        verify(sseNotificationService).sendNotificationToUser(eq(testUserId), any(Notification.class));
    }

    @Test
    void getNotificationStats_ShouldReturnStatistics() {
        // Given
        List<Notification> allNotifications = Arrays.asList(testNotification, testNotification);
        List<Notification> unreadNotifications = Arrays.asList(testNotification);

        when(notificationRepository.findByUserId(testUserId)).thenReturn(allNotifications);
        when(notificationRepository.findByUserIdAndIsRead(testUserId, false)).thenReturn(unreadNotifications);
        when(sseNotificationService.getUserConnectionCount(testUserId)).thenReturn(2);

        // When
        Map<String, Object> stats = notificationService.getNotificationStats(testUserId);

        // Then
        assertThat(stats.get("totalNotifications")).isEqualTo(2);
        assertThat(stats.get("unreadCount")).isEqualTo(1);
        assertThat(stats.get("readCount")).isEqualTo(1);
        assertThat(stats.get("sseConnections")).isEqualTo(2);
        assertThat(stats.get("lastNotification")).isNotNull();
    }
}