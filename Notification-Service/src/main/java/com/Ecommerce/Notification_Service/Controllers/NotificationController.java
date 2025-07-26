package com.Ecommerce.Notification_Service.Controllers;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Payload.Request.NotificationRequest;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable String userId) {
        log.info("Getting notifications for user: {}", userId);

        // Validate userId format
        if (!isValidUserId(userId)) {
            log.error("Invalid userId format: {}. Expected MongoDB ObjectId (24 hex characters)", userId);
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        UUID userUUID = parseToUUID(userId);
        List<Notification> notifications = notificationService.getAllNotificationsByUserId(userUUID);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable String userId) {
        log.info("Getting unread notifications for user: {}", userId);

        // Validate userId format
        if (!isValidUserId(userId)) {
            log.error("Invalid userId format: {}. Expected MongoDB ObjectId (24 hex characters)", userId);
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        UUID userUUID = parseToUUID(userId);
        List<Notification> notifications = notificationService.getUnreadNotificationsByUserId(userUUID);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/send")
    public ResponseEntity<Notification> sendNotification(@RequestBody NotificationRequest request) {
        log.info("Sending notification to user: {}", request.getUserId());

        Notification notification = notificationService.sendNotification(
                request.getUserId(),
                request.getType(),
                request.getContent(),
                request.getExpiresAt()
        );
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id) {
        log.info("Marking notification as read: {}", id);

        Notification notification = notificationService.markAsRead(id);
        if (notification != null) {
            return ResponseEntity.ok(notification);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        log.info("Deleting notification: {}", id);

        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserNotificationStats(@PathVariable String userId) {
        log.info("Getting notification stats for user: {}", userId);

        // Validate userId format
        if (!isValidUserId(userId)) {
            log.error("Invalid userId format: {}. Expected MongoDB ObjectId (24 hex characters)", userId);
            throw new IllegalArgumentException("Invalid userId format. Expected MongoDB ObjectId");
        }

        UUID userUUID = parseToUUID(userId);
        Map<String, Object> stats = notificationService.getNotificationStats(userUUID);
        return ResponseEntity.ok(stats);
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
     * Exception handler for IllegalArgumentException
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
     * Exception handler for general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("Unexpected error in notification controller", e);
        return ResponseEntity.status(500).body(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred",
                "timestamp", System.currentTimeMillis()
        ));
    }
}