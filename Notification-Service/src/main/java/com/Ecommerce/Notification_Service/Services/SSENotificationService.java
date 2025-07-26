package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Payload.Response.NotificationSSEDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SSENotificationService {

    private final Map<UUID, List<SseEmitter>> userConnections = new ConcurrentHashMap<>();
    private final Map<SseEmitter, UUID> emitterToUser = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    // Add this method to your SSENotificationService class

    /**
     * Get all connected user IDs
     */
    public Set<UUID> getConnectedUsers() {
        return new HashSet<>(userConnections.keySet());
    }

    /**
     * Check if a specific user is connected
     */
    public boolean isUserConnected(UUID userId) {
        List<SseEmitter> connections = userConnections.get(userId);
        return connections != null && !connections.isEmpty();
    }

    /**
     * Get all user connections for admin purposes
     */
    public Map<UUID, Integer> getUserConnectionCounts() {
        Map<UUID, Integer> connectionCounts = new HashMap<>();
        for (Map.Entry<UUID, List<SseEmitter>> entry : userConnections.entrySet()) {
            connectionCounts.put(entry.getKey(), entry.getValue().size());
        }
        return connectionCounts;
    }

    /**
     * Send notification to multiple specific users
     */
    public void sendNotificationToUsers(Set<UUID> userIds, Notification notification) {
        for (UUID userId : userIds) {
            sendNotificationToUser(userId, notification);
        }
    }

    /**
     * Send custom SSE message to specific user
     */
    public void sendCustomMessageToUser(UUID userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = userConnections.get(userId);

        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("No SSE connections found for user: {}", userId);
            return;
        }

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error serializing custom message for SSE: {}", eventName, e);
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonData));

                log.debug("Custom message sent via SSE to user: {} - event: {}", userId, eventName);
            } catch (IOException e) {
                log.warn("Failed to send custom SSE message to user: {} - marking emitter as dead", userId, e);
                deadEmitters.add(emitter);
            }
        }

        // Remove dead connections
        for (SseEmitter deadEmitter : deadEmitters) {
            removeConnection(deadEmitter);
        }
    }
    /**
     * Create SSE connection for a user
     */
    public SseEmitter createConnection(UUID userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // No timeout

        // Add user connection
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitterToUser.put(emitter, userId);

        log.info("SSE connection created for user: {}", userId);

        // Handle connection completion and errors
        emitter.onCompletion(() -> {
            removeConnection(emitter);
            log.info("SSE connection completed for user: {}", userId);
        });

        emitter.onTimeout(() -> {
            removeConnection(emitter);
            log.info("SSE connection timed out for user: {}", userId);
        });

        emitter.onError((ex) -> {
            removeConnection(emitter);
            log.error("SSE connection error for user: {}", userId, ex);
        });

        // Send initial connection message
        try {
            NotificationSSEDTO connectionMessage = NotificationSSEDTO.builder()
                    .id("connection-" + UUID.randomUUID().toString())
                    .type("CONNECTION_ESTABLISHED")
                    .title("Connected")
                    .message("Real-time notifications connected")
                    .timestamp(LocalDateTime.now())
                    .build();

            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data(objectMapper.writeValueAsString(connectionMessage)));
        } catch (IOException e) {
            log.error("Error sending initial SSE message to user: {}", userId, e);
            removeConnection(emitter);
        }

        return emitter;
    }

    /**
     * Send notification to specific user via SSE
     */
    public void sendNotificationToUser(UUID userId, Notification notification) {
        List<SseEmitter> userEmitters = userConnections.get(userId);

        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("No SSE connections found for user: {}", userId);
            return;
        }

        NotificationSSEDTO sseNotification = convertToSSEDTO(notification);
        String jsonData;

        try {
            jsonData = objectMapper.writeValueAsString(sseNotification);
        } catch (Exception e) {
            log.error("Error serializing notification for SSE: {}", notification.getId(), e);
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(jsonData));

                log.debug("Notification sent via SSE to user: {} - {}", userId, notification.getId());
            } catch (IOException e) {
                log.warn("Failed to send SSE notification to user: {} - marking emitter as dead", userId, e);
                deadEmitters.add(emitter);
            }
        }

        // Remove dead connections
        for (SseEmitter deadEmitter : deadEmitters) {
            removeConnection(deadEmitter);
        }
    }

    /**
     * Send notification to all connected users
     */
    public void broadcastNotification(Notification notification) {
        if (userConnections.isEmpty()) {
            log.debug("No SSE connections available for broadcast");
            return;
        }

        log.info("Broadcasting notification to {} users", userConnections.size());

        for (UUID userId : userConnections.keySet()) {
            sendNotificationToUser(userId, notification);
        }
    }

    /**
     * Send system alert to all users
     */
    public void sendSystemAlert(String title, String message, String alertType) {
        NotificationSSEDTO systemAlert = NotificationSSEDTO.builder()
                .id("system-" + UUID.randomUUID().toString())
                .type("SYSTEM_ALERT")
                .title(title)
                .message(message)
                .priority("HIGH")
                .category(alertType)
                .timestamp(LocalDateTime.now())
                .build();

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(systemAlert);
        } catch (Exception e) {
            log.error("Error serializing system alert for SSE", e);
            return;
        }

        for (List<SseEmitter> userEmitters : userConnections.values()) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("system-alert")
                            .data(jsonData));
                } catch (IOException e) {
                    log.warn("Failed to send system alert via SSE", e);
                }
            }
        }

        log.info("System alert broadcasted: {}", title);
    }

    /**
     * Get connection count for a user
     */
    public int getUserConnectionCount(UUID userId) {
        List<SseEmitter> connections = userConnections.get(userId);
        return connections != null ? connections.size() : 0;
    }

    /**
     * Get total active connections
     */
    public int getTotalConnections() {
        return userConnections.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Get connected users count
     */
    public int getConnectedUsersCount() {
        return userConnections.size();
    }

    /**
     * Remove a connection
     */
    private void removeConnection(SseEmitter emitter) {
        UUID userId = emitterToUser.remove(emitter);
        if (userId != null) {
            List<SseEmitter> userEmitters = userConnections.get(userId);
            if (userEmitters != null) {
                userEmitters.remove(emitter);
                if (userEmitters.isEmpty()) {
                    userConnections.remove(userId);
                }
            }
        }
    }

    /**
     * Convert Notification to SSE DTO
     */
    private NotificationSSEDTO convertToSSEDTO(Notification notification) {
        return NotificationSSEDTO.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(generateNotificationTitle(notification))
                .message(notification.getContent())
                .priority(determinePriority(notification.getType()))
                .category(determineCategory(notification.getType()))
                .timestamp(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .isRead(notification.isRead())
                .build();
    }

    /**
     * Generate appropriate title for notification based on type
     */
    private String generateNotificationTitle(Notification notification) {
        return switch (notification.getType()) {
            case INVENTORY_LOW_STOCK -> "Low Stock Alert";
            case INVENTORY_OUT_OF_STOCK -> "Out of Stock Alert";
            case INVENTORY_RESTOCKED -> "Stock Replenished";
            case PRODUCT_STATUS_CHANGED -> "Product Status Update";
            case PRODUCT_PRICE_CHANGED -> "Price Update";
            case PRODUCT_CREATED -> "New Product Added";
            case PRODUCT_DELETED -> "Product Removed";
            case DISCOUNT_ACTIVATED -> "New Discount Available";
            case DISCOUNT_EXPIRED -> "Discount Expired";
            case ORDER_STATUS -> "Order Update";
            case PAYMENT_CONFIRMATION -> "Payment Confirmed";
            case SHIPPING_UPDATE -> "Shipping Update";
            case SYSTEM_ALERT -> "System Alert";
            default -> "Notification";
        };
    }

    /**
     * Determine priority based on notification type
     */
    private String determinePriority(com.Ecommerce.Notification_Service.Models.NotificationType type) {
        return switch (type) {
            case INVENTORY_OUT_OF_STOCK, SYSTEM_ALERT -> "HIGH";
            case INVENTORY_LOW_STOCK, PRODUCT_STATUS_CHANGED, PAYMENT_CONFIRMATION -> "MEDIUM";
            default -> "LOW";
        };
    }

    /**
     * Determine category based on notification type
     */
    private String determineCategory(com.Ecommerce.Notification_Service.Models.NotificationType type) {
        return switch (type) {
            case INVENTORY_LOW_STOCK, INVENTORY_OUT_OF_STOCK, INVENTORY_RESTOCKED, INVENTORY_THRESHOLD_CHANGED -> "INVENTORY";
            case PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED, PRODUCT_STATUS_CHANGED, PRODUCT_PRICE_CHANGED, PRODUCT_STOCK_CHANGED -> "PRODUCT";
            case DISCOUNT_ACTIVATED, DISCOUNT_DEACTIVATED, DISCOUNT_EXPIRED, DISCOUNT_CREATED -> "DISCOUNT";
            case ORDER_STATUS, PAYMENT_CONFIRMATION, SHIPPING_UPDATE -> "ORDER";
            case SYSTEM_ALERT -> "SYSTEM";
            default -> "GENERAL";
        };
    }

    /**
     * Close all connections for a user (e.g., when user logs out)
     */
    public void closeUserConnections(UUID userId) {
        List<SseEmitter> userEmitters = userConnections.remove(userId);
        if (userEmitters != null) {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.complete();
                    emitterToUser.remove(emitter);
                } catch (Exception e) {
                    log.warn("Error closing SSE connection for user: {}", userId, e);
                }
            }
            log.info("Closed {} SSE connections for user: {}", userEmitters.size(), userId);
        }
    }

    /**
     * Send heartbeat to maintain connections
     */
    public void sendHeartbeat() {
        if (userConnections.isEmpty()) {
            return;
        }

        String heartbeatData = "{\"type\":\"HEARTBEAT\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (Map.Entry<UUID, List<SseEmitter>> entry : userConnections.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(heartbeatData));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
        }

        // Remove dead connections
        for (SseEmitter deadEmitter : deadEmitters) {
            removeConnection(deadEmitter);
        }

        log.debug("Heartbeat sent to {} active connections", getTotalConnections());
    }
}