package com.Ecommerce.Gateway_Service.Kafka;

public class KafkaTopics {
    // Request topics
    public static final String CART_REQUEST = "cart.request";
    public static final String PRODUCT_BATCH_REQUEST = "product.batch.request";
    public static final String Saved4later_REQUEST = "saved4later.request";

    // Response topics
    public static final String CART_RESPONSE = "cart.response";
    public static final String PRODUCT_BATCH_RESPONSE = "product.batch.response";
    public static final String Saved4later_RESPONSE = "saved4later.response";

    // Error topics
    public static final String CART_ERROR = "cart.error";
    public static final String PRODUCT_ERROR = "product.error";
    public static final String Saved4later_ERROR = "saved4later.error";
}