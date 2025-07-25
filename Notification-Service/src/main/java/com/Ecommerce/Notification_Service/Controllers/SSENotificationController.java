package com.Ecommerce.Notification_Service.Controllers;

import com.Ecommerce.Notification_Service.Services.SSENotificationService;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sse")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class SSENotificationController {

    @Autowired
    private SSENotificationService sseNotificationService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create SSE connection for a user
     */
    @GetMapping(value = "/connect/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable UUID userId) {
        log.info("Creating SSE connection for user: {}", userId);
        return sseNotificationService.createConnection(userId);
    }

    /**
     * Send system alert to all connected users
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastAlert(
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(defaultValue = "SYSTEM") String alertType) {

        log.info("Broadcasting system alert: {} - {}", title, message);
        sseNotificationService.sendSystemAlert(title, message, alertType);

        return ResponseEntity.ok(Map.of(
                "message", "System alert broadcasted successfully",
                "connectedUsers", sseNotificationService.getConnectedUsersCount(),
                "totalConnections", sseNotificationService.getTotalConnections()
        ));
    }

    /**
     * Close all connections for a user (e.g., when user logs out)
     */
    @PostMapping("/disconnect/{userId}")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable UUID userId) {
        log.info("Closing SSE connections for user: {}", userId);
        sseNotificationService.closeUserConnections(userId);

        return ResponseEntity.ok(Map.of(
                "message", "User connections closed successfully",
                "userId", userId
        ));
    }

    /**
     * Get SSE connection statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        return ResponseEntity.ok(Map.of(
                "connectedUsers", sseNotificationService.getConnectedUsersCount(),
                "totalConnections", sseNotificationService.getTotalConnections()
        ));
    }

    /**
     * Get connection count for specific user
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserConnectionStats(@PathVariable UUID userId) {
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "connectionCount", sseNotificationService.getUserConnectionCount(userId),
                "notificationStats", notificationService.getNotificationStats(userId)
        ));
    }

    /**
     * Health check endpoint for SSE service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "SSE Notification Service",
                "connectedUsers", sseNotificationService.getConnectedUsersCount(),
                "totalConnections", sseNotificationService.getTotalConnections(),
                "timestamp", System.currentTimeMillis()
        ));
    }
}