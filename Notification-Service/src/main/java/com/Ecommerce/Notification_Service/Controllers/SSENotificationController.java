package com.Ecommerce.Notification_Service.Controllers;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Services.SSENotificationService;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/sse")
@Slf4j
public class SSENotificationController {

    @Autowired
    private SSENotificationService sseNotificationService;

    @Autowired
    private NotificationService notificationService;

    // Track active connections and their heartbeat tasks
    private final ConcurrentHashMap<UUID, Set<SseEmitter>> activeConnections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SseEmitter, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(10);

    /**
     * Create SSE connection for a user with improved error handling
     */
    @GetMapping(value = "/connect/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String userId,
                              @RequestParam(required = false) String token,
                              HttpServletRequest request) {
        log.info("Creating SSE connection for user: {}", userId);

        // Validate userId format (MongoDB ObjectId: 24 hex characters)
        if (!isValidUserId(userId)) {
            log.error("Invalid userId format: {}. Expected MongoDB ObjectId (24 hex characters)", userId);
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        // Handle authentication - check both header and query param
        String authToken = extractAuthToken(request, token);
        if (authToken == null) {
            log.error("No authentication token provided for user: {}", userId);
            throw new IllegalArgumentException("Authentication token is required");
        }

        // Convert MongoDB ObjectId string to UUID for service
        UUID userUUID = parseToUUID(userId);
        log.info("Creating SSE connection for UUID: {}", userUUID);

        // Create emitter with longer timeout and better error handling
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout

        try {
            // Add connection tracking
            activeConnections.computeIfAbsent(userUUID, k -> ConcurrentHashMap.newKeySet()).add(emitter);

            // Set up completion handlers before registering with service
            emitter.onCompletion(() -> {
                log.info("SSE connection completed for user: {} (UUID: {})", userId, userUUID);
                cleanupConnection(userUUID, emitter);
            });

            emitter.onTimeout(() -> {
                log.warn("SSE connection timed out for user: {} (UUID: {})", userId, userUUID);
                cleanupConnection(userUUID, emitter);
            });

            emitter.onError((ex) -> {
                log.error("SSE connection error for user: {} (UUID: {}): {}", userId, userUUID, ex.getMessage());
                cleanupConnection(userUUID, emitter);
            });

            // Register with SSE service AFTER setting up handlers
            sseNotificationService.createConnection(userUUID);

            // Send initial connection confirmation
            try {
                emitter.send(SseEmitter.event()
                        .name("connection")
                        .data("{\"status\":\"connected\",\"userId\":\"" + userId + "\",\"timestamp\":" + System.currentTimeMillis() + "}")
                        .id(String.valueOf(System.currentTimeMillis())));
                log.info("Initial connection event sent to user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to send initial connection event: {}", e.getMessage());
                throw e; // Re-throw to trigger cleanup
            }

            // Start heartbeat with proper cleanup
            startHeartbeat(emitter, userUUID, userId);

            log.info("SSE emitter created successfully for user: {} (UUID: {})", userId, userUUID);
            return emitter;

        } catch (Exception e) {
            log.error("Error creating SSE connection for user: {}", userId, e);
            cleanupConnection(userUUID, emitter);
            throw new RuntimeException("Failed to create SSE connection: " + e.getMessage());
        }
    }
    /**
     * Send a test notification to a specific user
     */
    @PostMapping("/test/{userId}")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@PathVariable String userId) {
        if (!isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        try {
            UUID userUUID = parseToUUID(userId);

            // Create a proper Notification object
            Notification testData = new Notification();
            testData.setId("test-" + System.currentTimeMillis());
            testData.setUserId(userUUID);
            testData.setRead(false);

            sseNotificationService.sendNotificationToUser(userUUID, testData);

            return ResponseEntity.ok(Map.of(
                    "message", "Test notification sent successfully",
                    "userId", userId,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Error sending test notification to user: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to send test notification",
                    "message", e.getMessage()
            ));
        }
    }
    /**
     * Extract authentication token from request
     */
    private String extractAuthToken(HttpServletRequest request, String queryToken) {
        // First try Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fallback to query parameter
        if (queryToken != null && !queryToken.trim().isEmpty()) {
            return queryToken;
        }

        // Try cookie
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("token".equals(cookie.getName()) || "auth-token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Start heartbeat to keep connection alive with proper cleanup
     */
    private void startHeartbeat(SseEmitter emitter, UUID userUUID, String userId) {
        ScheduledFuture<?> heartbeatTask = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                // Check if connection still exists
                Set<SseEmitter> userConnections = activeConnections.get(userUUID);
                if (userConnections == null || !userConnections.contains(emitter)) {
                    return; // Connection was removed, task will be cancelled
                }

                // Send heartbeat
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("{\"timestamp\":" + System.currentTimeMillis() + "}")
                        .id("heartbeat-" + System.currentTimeMillis()));

                log.debug("Heartbeat sent to user: {}", userId);

            } catch (IOException e) {
                log.warn("Heartbeat failed for user: {}, cleaning up connection", userId);
                cleanupConnection(userUUID, emitter);
            } catch (Exception e) {
                log.error("Unexpected error in heartbeat for user: {}", userId, e);
                cleanupConnection(userUUID, emitter);
            }
        }, 30, 30, TimeUnit.SECONDS);

        // Store the task for cleanup
        heartbeatTasks.put(emitter, heartbeatTask);
    }

    /**
     * Clean up connection when it's closed or fails
     */
    private void cleanupConnection(UUID userUUID, SseEmitter emitter) {
        // Cancel heartbeat task
        ScheduledFuture<?> heartbeatTask = heartbeatTasks.remove(emitter);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }

        // Remove from active connections
        Set<SseEmitter> userConnections = activeConnections.get(userUUID);
        if (userConnections != null) {
            userConnections.remove(emitter);
            if (userConnections.isEmpty()) {
                activeConnections.remove(userUUID);
            }
        }

        // Complete the emitter if not already completed
        try {
            emitter.complete();
        } catch (Exception e) {
            log.debug("Emitter already completed or error completing: {}", e.getMessage());
        }


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

        try {
            sseNotificationService.sendSystemAlert(title, message, alertType);
            return ResponseEntity.ok(Map.of(
                    "message", "System alert broadcasted successfully",
                    "connectedUsers", sseNotificationService.getConnectedUsersCount(),
                    "totalConnections", sseNotificationService.getTotalConnections()
            ));
        } catch (Exception e) {
            log.error("Error broadcasting system alert", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to broadcast alert",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Close all connections for a user (e.g., when user logs out)
     */
    @PostMapping("/disconnect/{userId}")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable String userId) {
        log.info("Closing SSE connections for user: {}", userId);

        if (!isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        try {
            UUID userUUID = parseToUUID(userId);

            // Close connections in our tracking
            Set<SseEmitter> userConnections = activeConnections.remove(userUUID);
            int connectionsRemoved = 0;
            if (userConnections != null) {
                connectionsRemoved = userConnections.size();
                for (SseEmitter emitter : userConnections) {
                    cleanupConnection(userUUID, emitter);
                }
            }

            // Close connections in service
            sseNotificationService.closeUserConnections(userUUID);

            return ResponseEntity.ok(Map.of(
                    "message", "User connections closed successfully",
                    "userId", userId,
                    "connectionsRemoved", connectionsRemoved
            ));
        } catch (Exception e) {
            log.error("Error disconnecting user: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to disconnect user",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get SSE connection statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        try {
            return ResponseEntity.ok(Map.of(
                    "connectedUsers", sseNotificationService.getConnectedUsersCount(),
                    "totalConnections", sseNotificationService.getTotalConnections(),
                    "activeConnections", activeConnections.size(),
                    "heartbeatTasks", heartbeatTasks.size(),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Error getting connection stats", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get stats",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get connection count for specific user
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserConnectionStats(@PathVariable String userId) {
        if (!isValidUserId(userId)) {
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        try {
            UUID userUUID = parseToUUID(userId);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "connectionCount", sseNotificationService.getUserConnectionCount(userUUID),
                    "activeTrackedConnections", activeConnections.getOrDefault(userUUID, Set.of()).size(),
                    "notificationStats", notificationService.getNotificationStats(userUUID)
            ));
        } catch (Exception e) {
            log.error("Error getting user connection stats for user: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get user stats",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint for SSE service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "SSE Notification Service",
                    "connectedUsers", sseNotificationService.getConnectedUsersCount(),
                    "totalConnections", sseNotificationService.getTotalConnections(),
                    "activeTrackedConnections", activeConnections.size(),
                    "heartbeatTasks", heartbeatTasks.size(),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Health check failed", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * Validate MongoDB ObjectId format (24 hexadecimal characters)
     */
    private boolean isValidUserId(String userId) {
        if (userId == null || userId.length() != 24) {
            return false;
        }
        return userId.matches("[0-9a-fA-F]{24}");
    }

    /**
     * Convert MongoDB ObjectId string to UUID
     */
    private UUID parseToUUID(String mongoId) {
        try {
            String paddedId = String.format("%-32s", mongoId).replace(' ', '0');
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
     * Exception handler for IllegalArgumentException - NO LONGER USES text/event-stream
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
     * Exception handler for general exceptions - FIXED to not conflict with SSE
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error in SSE controller for path: {}", request.getRequestURI(), e);

        // For SSE endpoints, don't try to return JSON response as it conflicts with text/event-stream
        String requestURI = request.getRequestURI();
        if (requestURI != null && requestURI.contains("/connect/")) {
            // For SSE connect endpoints, log the error but don't return a response
            // The connection will be cleaned up by the onError handler
            return null;
        }

        return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Shutdown cleanup
     */
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        log.info("Shutting down SSE controller");

        // Cancel all heartbeat tasks
        heartbeatTasks.values().forEach(task -> task.cancel(true));
        heartbeatTasks.clear();

        // Close all connections
        activeConnections.forEach((userUUID, emitters) -> {
            emitters.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("Error completing emitter during shutdown: {}", e.getMessage());
                }
            });
        });
        activeConnections.clear();

        // Shutdown executor
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
        }
    }
}