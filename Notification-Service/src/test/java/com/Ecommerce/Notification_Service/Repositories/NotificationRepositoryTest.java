package com.Ecommerce.Notification_Service.Repositories;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
class NotificationRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID testUserId1;
    private UUID testUserId2;
    private Notification notification1;
    private Notification notification2;
    private Notification notification3;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        testUserId1 = UUID.randomUUID();
        testUserId2 = UUID.randomUUID();

        notification1 = new Notification(
                testUserId1,
                NotificationType.ORDER_STATUS,
                "Order status updated",
                LocalDateTime.now().plusDays(1)
        );

        notification2 = new Notification(
                testUserId1,
                NotificationType.PAYMENT_CONFIRMATION,
                "Payment confirmed",
                LocalDateTime.now().plusDays(2)
        );
        notification2.setRead(true);

        notification3 = new Notification(
                testUserId2,
                NotificationType.SHIPPING_UPDATE,
                "Shipping update",
                LocalDateTime.now().plusDays(3)
        );
    }

    @Test
    void save_ShouldPersistNotification() {
        // When
        Notification saved = notificationRepository.save(notification1);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(testUserId1);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_STATUS);
        assertThat(saved.getContent()).isEqualTo("Order status updated");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    void findById_WithExistingId_ShouldReturnNotification() {
        // Given
        Notification saved = notificationRepository.save(notification1);

        // When
        Optional<Notification> found = notificationRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUserId()).isEqualTo(testUserId1);
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        // When
        Optional<Notification> found = notificationRepository.findById("non-existent-id");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId_ShouldReturnUserNotifications() {
        // Given
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);

        // When
        List<Notification> userNotifications = notificationRepository.findByUserId(testUserId1);

        // Then
        assertThat(userNotifications).hasSize(2);
        assertThat(userNotifications)
                .extracting(Notification::getUserId)
                .containsOnly(testUserId1);
        assertThat(userNotifications)
                .extracting(Notification::getType)
                .containsExactlyInAnyOrder(NotificationType.ORDER_STATUS, NotificationType.PAYMENT_CONFIRMATION);
    }

    @Test
    void findByUserId_WithNoNotifications_ShouldReturnEmptyList() {
        // Given
        UUID userWithNoNotifications = UUID.randomUUID();

        // When
        List<Notification> notifications = notificationRepository.findByUserId(userWithNoNotifications);

        // Then
        assertThat(notifications).isEmpty();
    }

    @Test
    void findByUserIdAndIsRead_WithUnreadNotifications_ShouldReturnUnreadOnly() {
        // Given
        notificationRepository.save(notification1); // unread
        notificationRepository.save(notification2); // read
        notificationRepository.save(notification3); // unread, different user

        // When
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsRead(testUserId1, false);

        // Then
        assertThat(unreadNotifications).hasSize(1);
        assertThat(unreadNotifications.get(0).getUserId()).isEqualTo(testUserId1);
        assertThat(unreadNotifications.get(0).isRead()).isFalse();
        assertThat(unreadNotifications.get(0).getType()).isEqualTo(NotificationType.ORDER_STATUS);
    }

    @Test
    void findByUserIdAndIsRead_WithReadNotifications_ShouldReturnReadOnly() {
        // Given
        notificationRepository.save(notification1); // unread
        notificationRepository.save(notification2); // read
        notificationRepository.save(notification3); // unread, different user

        // When
        List<Notification> readNotifications = notificationRepository.findByUserIdAndIsRead(testUserId1, true);

        // Then
        assertThat(readNotifications).hasSize(1);
        assertThat(readNotifications.get(0).getUserId()).isEqualTo(testUserId1);
        assertThat(readNotifications.get(0).isRead()).isTrue();
        assertThat(readNotifications.get(0).getType()).isEqualTo(NotificationType.PAYMENT_CONFIRMATION);
    }

    @Test
    void deleteById_ShouldRemoveNotification() {
        // Given
        Notification saved = notificationRepository.save(notification1);
        String notificationId = saved.getId();

        // When
        notificationRepository.deleteById(notificationId);

        // Then
        Optional<Notification> found = notificationRepository.findById(notificationId);
        assertThat(found).isEmpty();
    }

    @Test
    void update_ShouldModifyExistingNotification() {
        // Given
        Notification saved = notificationRepository.save(notification1);
        assertThat(saved.isRead()).isFalse();

        // When
        saved.setRead(true);
        saved.setContent("Updated content");
        Notification updated = notificationRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(updated.getUserId()).isEqualTo(testUserId1);
    }

    @Test
    void count_ShouldReturnCorrectCount() {
        // Given
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);

        // When
        long count = notificationRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void deleteAll_ShouldRemoveAllNotifications() {
        // Given
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);
        assertThat(notificationRepository.count()).isEqualTo(3);

        // When
        notificationRepository.deleteAll();

        // Then
        assertThat(notificationRepository.count()).isEqualTo(0);
    }

    @Test
    void findAll_ShouldReturnAllNotifications() {
        // Given
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);

        // When
        List<Notification> allNotifications = notificationRepository.findAll();

        // Then
        assertThat(allNotifications).hasSize(3);
        assertThat(allNotifications)
                .extracting(Notification::getUserId)
                .containsExactlyInAnyOrder(testUserId1, testUserId1, testUserId2);
    }

    @Test
    void notification_ShouldHaveCorrectDefaultValues() {
        // Given
        Notification notification = new Notification(
                testUserId1,
                NotificationType.PROMOTION,
                "Promotion content",
                LocalDateTime.now().plusDays(1)
        );

        // When
        Notification saved = notificationRepository.save(notification);

        // Then
        assertThat(saved.isRead()).isFalse(); // Default value
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void notification_WithNullExpiresAt_ShouldBeSaved() {
        // Given
        Notification notification = new Notification(
                testUserId1,
                NotificationType.SYSTEM_ALERT,
                "System alert",
                null // null expires at
        );

        // When
        Notification saved = notificationRepository.save(notification);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getExpiresAt()).isNull();
    }

    @Test
    void findByUserId_WithLargeDataset_ShouldPerformWell() {
        // Given - Create multiple notifications for performance testing
        for (int i = 0; i < 100; i++) {
            Notification notification = new Notification(
                    testUserId1,
                    NotificationType.values()[i % NotificationType.values().length],
                    "Content " + i,
                    LocalDateTime.now().plusDays(i % 30)
            );
            if (i % 2 == 0) {
                notification.setRead(true);
            }
            notificationRepository.save(notification);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<Notification> notifications = notificationRepository.findByUserId(testUserId1);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(notifications).hasSize(100);
        assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
    }
}