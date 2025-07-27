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
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/sse")
@Slf4j
public class SSENotificationController {

    @Autowired
    private SSENotificationService sseNotificationService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create SSE connection for a user
     * Accepts MongoDB ObjectId as String and converts to UUID for service
     */
    @GetMapping(value = "/connect/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String userId) {
        log.info("Creating SSE connection for user: {}", userId);

        // Validate userId format (MongoDB ObjectId: 24 hex characters)
        if (!isValidUserId(userId)) {
            log.error("Invalid userId format: {}. Expected MongoDB ObjectId (24 hex characters)", userId);
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        // Convert MongoDB ObjectId string to UUID for service
        UUID userUUID = parseToUUID(userId);

        log.info("Creating SSE connection for UUID: {}", userUUID);
        SseEmitter emitter = sseNotificationService.createConnection(userUUID);

        // Log connection creation
        log.info("SSE emitter created successfully for user: {} (UUID: {})", userId, userUUID);

        // Add immediate callback to check if connection is maintained
        emitter.onCompletion(() -> {
            log.warn("SSE connection completed/closed for user: {} (UUID: {})", userId, userUUID);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out for user: {} (UUID: {})", userId, userUUID);
        });

        emitter.onError((ex) -> {
            log.error("SSE connection error for user: {} (UUID: {})", userId, userUUID, ex);
        });

        return emitter;
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
     * Accepts MongoDB ObjectId as String and converts to UUID for service
     */
    @PostMapping("/disconnect/{userId}")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable String userId) {
        log.info("Closing SSE connections for user: {}", userId);

        // Validate userId format
        if (!isValidUserId(userId)) {
            log.error("Invalid userId format: {}. Expected MongoDB ObjectId (24 hex characters)", userId);
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        // Convert MongoDB ObjectId string to UUID for service
        UUID userUUID = parseToUUID(userId);
        sseNotificationService.closeUserConnections(userUUID);

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
     * Accepts MongoDB ObjectId as String and converts to UUID for service
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserConnectionStats(@PathVariable String userId) {
        // Validate userId format
        if (!isValidUserId(userId)) {
            log.error("Invalid userId format: {}. Expected MongoDB ObjectId (24 hex characters)", userId);
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        // Convert MongoDB ObjectId string to UUID for service
        UUID userUUID = parseToUUID(userId);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "connectionCount", sseNotificationService.getUserConnectionCount(userUUID),
                "notificationStats", notificationService.getNotificationStats(userUUID)
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

    /**
     * Debug endpoint to get detailed connection information
     */
    @GetMapping("/debug/connections")
    public ResponseEntity<Map<String, Object>> debugConnections() {
        Map<UUID, Integer> userConnectionCounts = sseNotificationService.getUserConnectionCounts();
        Set<UUID> connectedUsers = sseNotificationService.getConnectedUsers();

        return ResponseEntity.ok(Map.of(
                "connectedUsers", connectedUsers.size(),
                "totalConnections", sseNotificationService.getTotalConnections(),
                "userConnectionDetails", userConnectionCounts,
                "connectedUserIds", connectedUsers,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Debug endpoint to check if specific user is connected
     */
    @GetMapping("/debug/user/{userId}")
    public ResponseEntity<Map<String, Object>> debugUserConnection(@PathVariable String userId) {
        if (!isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        UUID userUUID = parseToUUID(userId);
        boolean isConnected = sseNotificationService.isUserConnected(userUUID);
        int connectionCount = sseNotificationService.getUserConnectionCount(userUUID);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "userUUID", userUUID,
                "isConnected", isConnected,
                "connectionCount", connectionCount,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Validate MongoDB ObjectId format (24 hexadecimal characters)
     */
    private boolean isValidUserId(String userId) {
        if (userId == null || userId.length() != 24) {
            return false;
        }

        // Check if all characters are hexadecimal
        return userId.matches("[0-9a-fA-F]{24}");
    }

    /**
     * Convert MongoDB ObjectId string to UUID
     * This creates a deterministic UUID from the ObjectId string
     */
    private UUID parseToUUID(String mongoId) {
        try {
            // Create a deterministic UUID from the MongoDB ObjectId
            // Take the first 32 characters (all 24 from ObjectId + padding) and format as UUID
            String paddedId = String.format("%-32s", mongoId).replace(' ', '0');

            // Insert hyphens to create UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            String uuidString = paddedId.substring(0, 8) + "-" +
                    paddedId.substring(8, 12) + "-" +
                    paddedId.substring(12, 16) + "-" +
                    paddedId.substring(16, 20) + "-" +
                    paddedId.substring(20, 32);

            UUID result = UUID.fromString(uuidString);
            log.debug("Converted MongoDB ObjectId {} to UUID {}", mongoId, result);
            return result;

        } catch (Exception e) {
            log.error("Error converting MongoDB ObjectId {} to UUID", mongoId, e);
            throw new IllegalArgumentException("Failed to parse userId: " + mongoId, e);
        }
    }

    /**
     * Convert UUID back to MongoDB ObjectId string (utility method)
     * Extracts the original ObjectId from the padded UUID
     */
    private String convertUUIDToMongoId(UUID uuid) {
        try {
            String uuidString = uuid.toString().replace("-", "");
            // Remove the padding zeros we added
            return uuidString.substring(0, 24);
        } catch (Exception e) {
            log.error("Error converting UUID {} back to MongoDB ObjectId", uuid, e);
            throw new IllegalArgumentException("Failed to convert UUID back to MongoDB ObjectId: " + uuid, e);
        }
    }

    /**
     * Exception handler for IllegalArgumentException (invalid userId format or UUID parsing)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Invalid argument: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid Request",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Exception handler for general parsing exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("Unexpected error in SSE controller", e);
        return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred",
                "timestamp", System.currentTimeMillis()
        ));
    }
}