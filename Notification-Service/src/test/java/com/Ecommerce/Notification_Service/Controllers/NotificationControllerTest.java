package com.Ecommerce.Notification_Service.Controllers;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Payload.Request.NotificationRequest;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private Notification testNotification;
    private String validMongoObjectId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        validMongoObjectId = "507f1f77bcf86cd799439011"; // Valid 24-character hex string
        testNotification = new Notification(
                testUserId,
                NotificationType.ORDER_STATUS,
                "Test notification content",
                LocalDateTime.now().plusDays(1)
        );
        testNotification.setId("test-notification-id");
    }

    @Test
    void getUserNotifications_WithValidUserId_ShouldReturnNotifications() throws Exception {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationService.getAllNotificationsByUserId(any(UUID.class))).thenReturn(notifications);

        // When & Then
        mockMvc.perform(get("/notifications/user/{userId}", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("test-notification-id")))
                .andExpect(jsonPath("$[0].type", is("ORDER_STATUS")))
                .andExpect(jsonPath("$[0].content", is("Test notification content")))
                .andExpect(jsonPath("$[0].read", is(false)));
    }

    @Test
    void getUserNotifications_WithInvalidUserId_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidUserId = "invalid-user-id";

        // When & Then
        mockMvc.perform(get("/notifications/user/{userId}", invalidUserId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid Request")))
                .andExpect(jsonPath("$.message", containsString("Invalid userId format")));
    }

    @Test
    void getUnreadNotifications_WithValidUserId_ShouldReturnUnreadNotifications() throws Exception {
        // Given
        List<Notification> unreadNotifications = Arrays.asList(testNotification);
        when(notificationService.getUnreadNotificationsByUserId(any(UUID.class))).thenReturn(unreadNotifications);

        // When & Then
        mockMvc.perform(get("/notifications/user/{userId}/unread", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].read", is(false)));
    }

    @Test
    void sendNotification_WithValidRequest_ShouldCreateNotification() throws Exception {
        // Given
        NotificationRequest request = new NotificationRequest();
        request.setUserId(testUserId);
        request.setType(NotificationType.ORDER_STATUS);
        request.setContent("Test notification content");
        request.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(notificationService.sendNotification(any(UUID.class), any(NotificationType.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(testNotification);

        // When & Then
        mockMvc.perform(post("/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("test-notification-id")))
                .andExpect(jsonPath("$.type", is("ORDER_STATUS")))
                .andExpect(jsonPath("$.content", is("Test notification content")));
    }

    @Test
    void markAsRead_WithValidId_ShouldMarkNotificationAsRead() throws Exception {
        // Given
        testNotification.setRead(true);
        when(notificationService.markAsRead("test-notification-id")).thenReturn(testNotification);

        // When & Then
        mockMvc.perform(put("/notifications/{id}/read", "test-notification-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("test-notification-id")))
                .andExpect(jsonPath("$.read", is(true)));
    }

    @Test
    void markAsRead_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Given
        when(notificationService.markAsRead("non-existent-id")).thenReturn(null);

        // When & Then
        mockMvc.perform(put("/notifications/{id}/read", "non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNotification_WithValidId_ShouldReturnNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/notifications/{id}", "test-notification-id"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUserNotificationStats_WithValidUserId_ShouldReturnStats() throws Exception {
        // Given
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotifications", 10);
        stats.put("unreadCount", 3);
        stats.put("readCount", 7);
        stats.put("sseConnections", 2);
        stats.put("lastNotification", LocalDateTime.now());

        when(notificationService.getNotificationStats(any(UUID.class))).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/notifications/user/{userId}/stats", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNotifications", is(10)))
                .andExpect(jsonPath("$.unreadCount", is(3)))
                .andExpect(jsonPath("$.readCount", is(7)))
                .andExpect(jsonPath("$.sseConnections", is(2)));
    }

    @Test
    void getUserNotificationStats_WithInvalidUserId_ShouldReturnBadRequest() throws Exception {
        // Given
        String invalidUserId = "12345"; // Too short for MongoDB ObjectId

        // When & Then
        mockMvc.perform(get("/notifications/user/{userId}/stats", invalidUserId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid Request")))
                .andExpect(jsonPath("$.message", containsString("Invalid userId format")));
    }

    @Test
    void sendNotification_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        NotificationRequest invalidRequest = new NotificationRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserNotifications_WithEmptyResult_ShouldReturnEmptyList() throws Exception {
        // Given
        when(notificationService.getAllNotificationsByUserId(any(UUID.class))).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/notifications/user/{userId}", validMongoObjectId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getUserNotifications_WithServiceException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(notificationService.getAllNotificationsByUserId(any(UUID.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/notifications/user/{userId}", validMongoObjectId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")));
    }
}