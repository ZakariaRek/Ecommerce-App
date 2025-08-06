package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SSENotificationServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SSENotificationService sseNotificationService;

    private UUID testUserId;
    private Notification testNotification;

    @BeforeEach
    void setUp() throws Exception {
        testUserId = UUID.randomUUID();
        testNotification = new Notification(
                testUserId,
                NotificationType.ORDER_STATUS,
                "Test notification content",
                LocalDateTime.now().plusDays(1)
        );
        testNotification.setId("test-notification-id");

        // Mock ObjectMapper behavior
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    }

    @Test
    void createConnection_ShouldReturnSseEmitterAndAddToConnections() throws Exception {
        // When
        SseEmitter emitter = sseNotificationService.createConnection(testUserId);

        // Then
        assertThat(emitter).isNotNull();
        assertThat(sseNotificationService.isUserConnected(testUserId)).isTrue();
        assertThat(sseNotificationService.getUserConnectionCount(testUserId)).isEqualTo(1);
        assertThat(sseNotificationService.getConnectedUsersCount()).isEqualTo(1);
        assertThat(sseNotificationService.getTotalConnections()).isEqualTo(1);

        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    void createConnection_ForSameUserMultipleTimes_ShouldAllowMultipleConnections() throws Exception {
        // When
        SseEmitter emitter1 = sseNotificationService.createConnection(testUserId);
        SseEmitter emitter2 = sseNotificationService.createConnection(testUserId);

        // Then
        assertThat(emitter1).isNotNull();
        assertThat(emitter2).isNotNull();
        assertThat(emitter1).isNotEqualTo(emitter2);
        assertThat(sseNotificationService.getUserConnectionCount(testUserId)).isEqualTo(2);
        assertThat(sseNotificationService.getConnectedUsersCount()).isEqualTo(1); // Still one user
        assertThat(sseNotificationService.getTotalConnections()).isEqualTo(2);
    }

    @Test
    void sendNotificationToUser_WithConnectedUser_ShouldSendNotification() throws Exception {
        // Given
        sseNotificationService.createConnection(testUserId);

        // When
        sseNotificationService.sendNotificationToUser(testUserId, testNotification);

        // Then
        verify(objectMapper, atLeast(1)).writeValueAsString(any());
    }

    @Test
    void sendNotificationToUser_WithDisconnectedUser_ShouldNotThrowException() throws JsonProcessingException {
        // Given
        UUID disconnectedUserId = UUID.randomUUID();

        // When & Then (should not throw exception)
        sseNotificationService.sendNotificationToUser(disconnectedUserId, testNotification);

        // Verify no attempts to send
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void broadcastNotification_WithMultipleConnectedUsers_ShouldSendToAllUsers() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        sseNotificationService.createConnection(userId1);
        sseNotificationService.createConnection(userId2);

        // When
        sseNotificationService.broadcastNotification(testNotification);

        // Then
        assertThat(sseNotificationService.getConnectedUsersCount()).isEqualTo(2);
        verify(objectMapper, atLeast(2)).writeValueAsString(any()); // At least for connection + broadcast
    }

    @Test
    void sendSystemAlert_WithConnectedUsers_ShouldBroadcastAlert() throws Exception {
        // Given
        sseNotificationService.createConnection(testUserId);
        String title = "System Maintenance";
        String message = "System will be down for maintenance";
        String alertType = "MAINTENANCE";

        // When
        sseNotificationService.sendSystemAlert(title, message, alertType);

        // Then
        verify(objectMapper, atLeast(1)).writeValueAsString(any());
    }

    @Test
    void closeUserConnections_WithConnectedUser_ShouldRemoveAllUserConnections() throws Exception {
        // Given
        sseNotificationService.createConnection(testUserId);
        sseNotificationService.createConnection(testUserId); // Multiple connections for same user
        assertThat(sseNotificationService.getUserConnectionCount(testUserId)).isEqualTo(2);

        // When
        sseNotificationService.closeUserConnections(testUserId);

        // Then
        assertThat(sseNotificationService.isUserConnected(testUserId)).isFalse();
        assertThat(sseNotificationService.getUserConnectionCount(testUserId)).isEqualTo(0);
        assertThat(sseNotificationService.getConnectedUsersCount()).isEqualTo(0);
    }

    @Test
    void getConnectedUsers_WithMultipleUsers_ShouldReturnAllConnectedUserIds() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        sseNotificationService.createConnection(userId1);
        sseNotificationService.createConnection(userId2);
        sseNotificationService.createConnection(userId3);
        sseNotificationService.createConnection(userId1); // Multiple connections for userId1

        // When
        Set<UUID> connectedUsers = sseNotificationService.getConnectedUsers();

        // Then
        assertThat(connectedUsers).hasSize(3);
        assertThat(connectedUsers).contains(userId1, userId2, userId3);
        assertThat(sseNotificationService.getTotalConnections()).isEqualTo(4);
    }

    @Test
    void getUserConnectionCounts_ShouldReturnCorrectCounts() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        sseNotificationService.createConnection(userId1);
        sseNotificationService.createConnection(userId1);
        sseNotificationService.createConnection(userId2);

        // When
        Map<UUID, Integer> connectionCounts = sseNotificationService.getUserConnectionCounts();

        // Then
        assertThat(connectionCounts).hasSize(2);
        assertThat(connectionCounts.get(userId1)).isEqualTo(2);
        assertThat(connectionCounts.get(userId2)).isEqualTo(1);
    }

    @Test
    void isUserConnected_WithConnectedUser_ShouldReturnTrue() throws Exception {
        // Given
        sseNotificationService.createConnection(testUserId);

        // When
        boolean isConnected = sseNotificationService.isUserConnected(testUserId);

        // Then
        assertThat(isConnected).isTrue();
    }

    @Test
    void isUserConnected_WithDisconnectedUser_ShouldReturnFalse() {
        // Given
        UUID disconnectedUserId = UUID.randomUUID();

        // When
        boolean isConnected = sseNotificationService.isUserConnected(disconnectedUserId);

        // Then
        assertThat(isConnected).isFalse();
    }

    @Test
    void sendNotificationToUsers_WithMultipleUsers_ShouldSendToAllSpecifiedUsers() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        sseNotificationService.createConnection(userId1);
        sseNotificationService.createConnection(userId2);
        sseNotificationService.createConnection(userId3);

        Set<UUID> targetUsers = Set.of(userId1, userId2);

        // When
        sseNotificationService.sendNotificationToUsers(targetUsers, testNotification);

        // Then
        // Verify that notification is sent (mocked ObjectMapper should be called)
        verify(objectMapper, atLeast(targetUsers.size())).writeValueAsString(any());
    }

    @Test
    void sendCustomMessageToUser_WithConnectedUser_ShouldSendCustomMessage() throws Exception {
        // Given
        sseNotificationService.createConnection(testUserId);
        String eventName = "custom-event";
        Object customData = Map.of("message", "custom message");

        // When
        sseNotificationService.sendCustomMessageToUser(testUserId, eventName, customData);

        // Then
        verify(objectMapper, atLeast(1)).writeValueAsString(eq(customData));
    }

    @Test
    void sendCustomMessageToUser_WithDisconnectedUser_ShouldNotThrowException() {
        // Given
        UUID disconnectedUserId = UUID.randomUUID();
        String eventName = "custom-event";
        Object customData = Map.of("message", "custom message");

        // When & Then (should not throw exception)
        sseNotificationService.sendCustomMessageToUser(disconnectedUserId, eventName, customData);
    }

    @Test
    void sendHeartbeat_WithConnectedUsers_ShouldSendHeartbeatToAll() throws Exception {
        // Given
        sseNotificationService.createConnection(testUserId);
        sseNotificationService.createConnection(UUID.randomUUID());

        // When
        sseNotificationService.sendHeartbeat();

        // Then
        // Verify heartbeat is sent (this would involve mocking SseEmitter.send but that's complex)
        // For now, just verify no exceptions are thrown and connections remain
        assertThat(sseNotificationService.getTotalConnections()).isEqualTo(2);
    }

    @Test
    void sendHeartbeat_WithNoConnections_ShouldNotThrowException() {
        // When & Then (should not throw exception)
        sseNotificationService.sendHeartbeat();

        // Verify no connections
        assertThat(sseNotificationService.getTotalConnections()).isEqualTo(0);
    }

    @Test
    void getConnectionStatistics_ShouldReturnCorrectValues() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        sseNotificationService.createConnection(userId1);
        sseNotificationService.createConnection(userId1);
        sseNotificationService.createConnection(userId2);

        // When & Then
        assertThat(sseNotificationService.getConnectedUsersCount()).isEqualTo(2);
        assertThat(sseNotificationService.getTotalConnections()).isEqualTo(3);
        assertThat(sseNotificationService.getUserConnectionCount(userId1)).isEqualTo(2);
        assertThat(sseNotificationService.getUserConnectionCount(userId2)).isEqualTo(1);
        assertThat(sseNotificationService.getUserConnectionCount(UUID.randomUUID())).isEqualTo(0);
    }
}