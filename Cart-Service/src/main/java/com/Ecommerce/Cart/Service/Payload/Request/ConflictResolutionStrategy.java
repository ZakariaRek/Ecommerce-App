package com.Ecommerce.Cart.Service.Payload.Request;

public enum ConflictResolutionStrategy {
    SUM_QUANTITIES,
    KEEP_LATEST,
    KEEP_SERVER,
    KEEP_LOCAL,
    ASK_USER
}