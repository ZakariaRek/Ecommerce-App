package com.Ecommerce.Notification_Service.Models;

public enum NotificationType {
    // Existing types
    ORDER_STATUS,
    ACCOUNT_ACTIVITY,
    PAYMENT_CONFIRMATION,
    SHIPPING_UPDATE,
    PRODUCT_RESTOCKED,


//    ACCOUNT_ACTIVITY,
    PROMOTION,

    // New Product-related types
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    PRODUCT_STATUS_CHANGED,
    PRODUCT_PRICE_CHANGED,
    PRODUCT_STOCK_CHANGED,

    // Inventory-related types
    INVENTORY_LOW_STOCK,
    INVENTORY_OUT_OF_STOCK,
    INVENTORY_RESTOCKED,
    INVENTORY_THRESHOLD_CHANGED,

    // Discount-related types
    DISCOUNT_ACTIVATED,
    DISCOUNT_DEACTIVATED,
    DISCOUNT_EXPIRED,
    DISCOUNT_CREATED,

    // Category-related types
    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_DELETED,

    // Review-related types
    REVIEW_CREATED,
    REVIEW_VERIFIED,

    // Supplier-related types
    SUPPLIER_CREATED,
    SUPPLIER_UPDATED,
    SUPPLIER_RATING_CHANGED,

    // System notifications
    SYSTEM_ALERT,
    MAINTENANCE_SCHEDULED
}