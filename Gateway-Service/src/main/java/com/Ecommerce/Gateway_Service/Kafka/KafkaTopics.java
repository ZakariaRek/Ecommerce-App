package com.Ecommerce.Gateway_Service.Kafka;

public class KafkaTopics {

    // Cart topics
    public static final String CART_REQUEST = "cart.request";
    public static final String CART_RESPONSE = "cart.response";
    public static final String CART_ERROR = "cart.error";

    // Product topics
    public static final String PRODUCT_BATCH_REQUEST = "product.batch.request";
    public static final String PRODUCT_BATCH_RESPONSE = "product.batch.response";
    public static final String PRODUCT_ERROR = "product.error";

    // Order topics
    public static final String ORDER_REQUEST = "order.request";
    public static final String ORDER_RESPONSE = "order.response";
    public static final String ORDER_ERROR = "order.error";

    // User topics (if needed in future)
    public static final String USER_REQUEST = "user.request";
    public static final String USER_RESPONSE = "user.response";
    public static final String USER_ERROR = "user.error";
}