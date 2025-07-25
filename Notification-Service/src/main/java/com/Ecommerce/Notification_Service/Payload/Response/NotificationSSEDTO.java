package com.Ecommerce.Notification_Service.Payload.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSSEDTO {

    private String id;
    private String type;
    private String title;
    private String message;
    private String priority; // HIGH, MEDIUM, LOW
    private String category; // INVENTORY, PRODUCT, ORDER, SYSTEM, etc.

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    private Boolean isRead;
    private String actionUrl; // Optional URL for action
    private String actionText; // Optional action button text

    // Additional metadata for frontend
    private String icon; // Icon class or name
    private String color; // Color code for UI
    private Boolean dismissible; // Can be dismissed by user
    private Integer duration; // Auto-dismiss duration in seconds (0 = persistent)

    // Product-specific fields (when applicable)
    private String productId;
    private String productName;
    private String productImage;

    // Inventory-specific fields (when applicable)
    private Integer currentStock;
    private Integer threshold;
    private String warehouseLocation;

    // Discount-specific fields (when applicable)
    private String discountValue;
    private String discountType;

    // Order-specific fields (when applicable)
    private String orderId;
    private String orderStatus;

    public static NotificationSSEDTO createInventoryAlert(String id, String productName, Integer currentStock, Integer threshold) {
        return NotificationSSEDTO.builder()
                .id(id)
                .type("INVENTORY_LOW_STOCK")
                .title("Low Stock Alert")
                .message(String.format("%s is running low (Stock: %d, Threshold: %d)", productName, currentStock, threshold))
                .priority("MEDIUM")
                .category("INVENTORY")
                .productName(productName)
                .currentStock(currentStock)
                .threshold(threshold)
                .icon("fa-exclamation-triangle")
                .color("#ff9800")
                .dismissible(true)
                .duration(0) // Persistent
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static NotificationSSEDTO createProductStatusAlert(String id, String productName, String oldStatus, String newStatus) {
        return NotificationSSEDTO.builder()
                .id(id)
                .type("PRODUCT_STATUS_CHANGED")
                .title("Product Status Update")
                .message(String.format("%s status changed from %s to %s", productName, oldStatus, newStatus))
                .priority("MEDIUM")
                .category("PRODUCT")
                .productName(productName)
                .icon("fa-product-hunt")
                .color("#2196f3")
                .dismissible(true)
                .duration(10)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static NotificationSSEDTO createDiscountAlert(String id, String productName, String discountValue, String discountType) {
        return NotificationSSEDTO.builder()
                .id(id)
                .type("DISCOUNT_ACTIVATED")
                .title("New Discount Available")
                .message(String.format("New %s discount of %s available for %s", discountType, discountValue, productName))
                .priority("LOW")
                .category("DISCOUNT")
                .productName(productName)
                .discountValue(discountValue)
                .discountType(discountType)
                .icon("fa-percent")
                .color("#4caf50")
                .dismissible(true)
                .duration(15)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static NotificationSSEDTO createSystemAlert(String id, String title, String message, String alertType) {
        return NotificationSSEDTO.builder()
                .id(id)
                .type("SYSTEM_ALERT")
                .title(title)
                .message(message)
                .priority("HIGH")
                .category("SYSTEM")
                .icon("fa-bell")
                .color("#f44336")
                .dismissible(false)
                .duration(0) // Persistent
                .timestamp(LocalDateTime.now())
                .build();
    }
}