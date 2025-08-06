package com.Ecommerce.Notification_Service.Integration;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Payload.Request.NotificationRequest;
import com.Ecommerce.Notification_Service.Repositories.NotificationRepository;
import com.Ecommerce.Notification_Service.Services.EmailService;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import com.Ecommerce.Notification_Service.Services.SSENotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@DirtiesContext
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "notification.email.from=test@example.com",
        "notification.email.from-name=Test Service",
        "notification.email.templates.base-url=http://localhost:3000"
})
class NotificationServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SSENotificationService sseNotificationService;

    @Mock
    private JavaMailSender mailSender;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private UUID testUserId;
    private String validMongoObjectId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testUserId = UUID.randomUUID();
        validMongoObjectId = "507f1f77bcf86cd799439011";

        // Clean database
        notificationRepository.deleteAll();

        // Mock mail sender
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void contextLoads() {
        assertThat(notificationService).isNotNull();
        assertThat(notificationRepository).isNotNull();
        assertThat(sseNotificationService).isNotNull();
    }

    @Test
    void createNotification_EndToEndFlow_ShouldWorkCorrectly() throws Exception {
        // Given
        NotificationRequest request = new NotificationRequest();
        request.setUserId(testUserId);
        request.setType(NotificationType.ORDER_STATUS);
        request.setContent("Integration test notification");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        // When - Create notification via REST API
        mockMvc.perform(post("/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("ORDER_STATUS")))
                .andExpect(jsonPath("$.content", is("Integration test notification")))
                .andExpect(jsonPath("$.read", is(false)));

        // Then - Verify notification was persisted in database
        List<Notification> notifications = notificationRepository.findByUserId(testUserId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getContent()).isEqualTo("Integration test notification");
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ORDER_STATUS);
        assertThat(notifications.get(0).isRead()).isFalse();
    }

    @Test
    void getNotifications_EndToEndFlow_ShouldReturnCorrectData() throws Exception {
        // Given - Create notifications in database
        Notification notification1 = notificationService.createNotification(
                testUserId, NotificationType.ORDER_STATUS, "Test notification 1", LocalDateTime.now().plusDays(1));
        Notification notification2 = notificationService.createNotification(
                testUserId, NotificationType.PAYMENT_CONFIRMATION, "Test notification 2", LocalDateTime.now().plusDays(2));

        // When & Then - Get notifications via REST API
        mockMvc.perform(get("/notifications/user/{userId}", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type", anyOf(is("ORDER_STATUS"), is("PAYMENT_CONFIRMATION"))))
                .andExpect(jsonPath("$[1].type", anyOf(is("ORDER_STATUS"), is("PAYMENT_CONFIRMATION"))));
    }

    @Test
    void markAsRead_EndToEndFlow_ShouldUpdateNotification() throws Exception {
        // Given - Create notification
        Notification notification = notificationService.createNotification(
                testUserId, NotificationType.ORDER_STATUS, "Test notification", LocalDateTime.now().plusDays(1));
        String notificationId = notification.getId();

        // When - Mark as read via REST API
        mockMvc.perform(put("/notifications/{id}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read", is(true)));

        // Then - Verify in database
        Notification updated = notificationRepository.findById(notificationId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.isRead()).isTrue();
    }

    @Test
    void deleteNotification_EndToEndFlow_ShouldRemoveFromDatabase() throws Exception {
        // Given - Create notification
        Notification notification = notificationService.createNotification(
                testUserId, NotificationType.ORDER_STATUS, "Test notification", LocalDateTime.now().plusDays(1));
        String notificationId = notification.getId();

        // When - Delete via REST API
        mockMvc.perform(delete("/notifications/{id}", notificationId))
                .andExpect(status().isNoContent());

        // Then - Verify removal from database
        assertThat(notificationRepository.findById(notificationId)).isEmpty();
    }

    @Test
    void getUnreadNotifications_EndToEndFlow_ShouldReturnOnlyUnread() throws Exception {
        // Given - Create notifications, mark one as read
        Notification notification1 = notificationService.createNotification(
                testUserId, NotificationType.ORDER_STATUS, "Unread notification", LocalDateTime.now().plusDays(1));
        Notification notification2 = notificationService.createNotification(
                testUserId, NotificationType.PAYMENT_CONFIRMATION, "Read notification", LocalDateTime.now().plusDays(2));

        // Mark second notification as read
        notificationService.markAsRead(notification2.getId());

        // When & Then - Get unread notifications
        mockMvc.perform(get("/notifications/user/{userId}/unread", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("Unread notification")))
                .andExpect(jsonPath("$[0].read", is(false)));
    }

    @Test
    void getUserNotificationStats_EndToEndFlow_ShouldReturnCorrectStats() throws Exception {
        // Given - Create multiple notifications
        notificationService.createNotification(testUserId, NotificationType.ORDER_STATUS, "Notification 1", LocalDateTime.now().plusDays(1));
        Notification readNotification = notificationService.createNotification(testUserId, NotificationType.PAYMENT_CONFIRMATION, "Notification 2", LocalDateTime.now().plusDays(2));
        notificationService.createNotification(testUserId, NotificationType.SHIPPING_UPDATE, "Notification 3", LocalDateTime.now().plusDays(3));

        // Mark one as read
        notificationService.markAsRead(readNotification.getId());

        // When & Then - Get stats
        mockMvc.perform(get("/notifications/user/{userId}/stats", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNotifications", is(3)))
                .andExpect(jsonPath("$.unreadCount", is(2)))
                .andExpect(jsonPath("$.readCount", is(1)))
                .andExpect(jsonPath("$.sseConnections", is(0))); // No SSE connections in test
    }

    @Test
    void sseConnection_EndToEndFlow_ShouldCreateConnection() throws Exception {
        // When - Create SSE connection
        mockMvc.perform(get("/sse/connect/{userId}", validMongoObjectId)
                        .accept("text/event-stream"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/event-stream")));

        // Then - Verify connection statistics
        mockMvc.perform(get("/sse/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectedUsers", is(1)))
                .andExpect(jsonPath("$.totalConnections", is(1)));
    }

    @Test
    void broadcastSystemAlert_EndToEndFlow_ShouldSendAlert() throws Exception {
        // When - Broadcast system alert
        mockMvc.perform(post("/sse/broadcast")
                        .param("title", "System Maintenance")
                        .param("message", "System will be down for maintenance")
                        .param("alertType", "MAINTENANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("System alert broadcasted successfully")));
    }

    @Test
    void emailTest_EndToEndFlow_ShouldConfigureEmailService() throws Exception {
        // When & Then - Test email configuration
        mockMvc.perform(post("/email/test/config")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("sent successfully")));
    }

    @Test
    void getEmailTypes_EndToEndFlow_ShouldReturnAvailableTypes() throws Exception {
        // When & Then - Get available email types
        mockMvc.perform(get("/email/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTypes", isA(List.class)))
                .andExpect(jsonPath("$.examples", isA(Map.class)));
    }

    @Test
    void sendTestEmail_EndToEndFlow_ShouldSendEmail() throws Exception {
        // When & Then - Send test email
        mockMvc.perform(post("/email/test/send-to-one")
                        .param("email", "test@example.com")
                        .param("type", "ORDER_STATUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void invalidUserId_ShouldReturnBadRequest() throws Exception {
        // When & Then - Use invalid userId format
        mockMvc.perform(get("/notifications/user/{userId}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid Request")))
                .andExpect(jsonPath("$.message", containsString("Invalid userId format")));
    }

    @Test
    void notificationWorkflow_CompleteFlow_ShouldWorkEndToEnd() throws Exception {
        // Step 1: Create notification
        NotificationRequest request = new NotificationRequest();
        request.setUserId(testUserId);
        request.setType(NotificationType.ORDER_STATUS);
        request.setContent("Complete workflow test");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        String createResponse = mockMvc.perform(post("/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Notification createdNotification = objectMapper.readValue(createResponse, Notification.class);
        String notificationId = createdNotification.getId();

        // Step 2: Verify it appears in user notifications
        mockMvc.perform(get("/notifications/user/{userId}", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(notificationId)));

        // Step 3: Verify it appears in unread notifications
        mockMvc.perform(get("/notifications/user/{userId}/unread", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Step 4: Mark as read
        mockMvc.perform(put("/notifications/{id}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read", is(true)));

        // Step 5: Verify it no longer appears in unread notifications
        mockMvc.perform(get("/notifications/user/{userId}/unread", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Step 6: Check stats
        mockMvc.perform(get("/notifications/user/{userId}/stats", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNotifications", is(1)))
                .andExpect(jsonPath("$.unreadCount", is(0)))
                .andExpect(jsonPath("$.readCount", is(1)));

        // Step 7: Delete notification
        mockMvc.perform(delete("/notifications/{id}", notificationId))
                .andExpect(status().isNoContent());

        // Step 8: Verify notification is gone
        mockMvc.perform(get("/notifications/user/{userId}", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void highVolumeNotifications_ShouldHandleCorrectly() throws Exception {
        // Given - Create many notifications
        int notificationCount = 50;
        for (int i = 0; i < notificationCount; i++) {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(testUserId);
            request.setType(NotificationType.values()[i % NotificationType.values().length]);
            request.setContent("High volume test notification " + i);
            request.setExpiresAt(LocalDateTime.now().plusDays(1));

            mockMvc.perform(post("/notifications/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // When & Then - Verify all notifications are retrievable
        mockMvc.perform(get("/notifications/user/{userId}", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(notificationCount)));

        // Verify stats
        mockMvc.perform(get("/notifications/user/{userId}/stats", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNotifications", is(notificationCount)))
                .andExpect(jsonPath("$.unreadCount", is(notificationCount)))
                .andExpect(jsonPath("$.readCount", is(0)));
    }
}