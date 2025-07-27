package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Payload.Kafka.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Enhanced service for communicating with User Service to fetch user information via Kafka
 * Updated to handle String-based deserialization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserEmailService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper; // Add ObjectMapper for manual deserialization

    // Cache for user emails (in-memory cache)
    private final Map<UUID, CachedUserEmail> emailCache = new ConcurrentHashMap<>();

    // Cache for full user information
    private final Map<UUID, CachedUserInfo> userInfoCache = new ConcurrentHashMap<>();

    // Pending requests (waiting for responses)
    private final Map<String, CompletableFuture<UserEmailResponse>> pendingEmailRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<BulkUserEmailResponse>> pendingBulkEmailRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<UserInfoResponse>> pendingUserInfoRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<BulkUserInfoResponse>> pendingBulkUserInfoRequests = new ConcurrentHashMap<>();

    // Kafka Topics
    public static final String TOPIC_USER_EMAIL_REQUEST = "user-email-request";
    public static final String TOPIC_USER_EMAIL_RESPONSE = "user-email-response";
    public static final String TOPIC_BULK_USER_EMAIL_REQUEST = "bulk-user-email-request";
    public static final String TOPIC_BULK_USER_EMAIL_RESPONSE = "bulk-user-email-response";
    public static final String TOPIC_USER_INFO_REQUEST = "user-info-request";
    public static final String TOPIC_USER_INFO_RESPONSE = "user-info-response";
    public static final String TOPIC_BULK_USER_INFO_REQUEST = "bulk-user-info-request";
    public static final String TOPIC_BULK_USER_INFO_RESPONSE = "bulk-user-info-response";

    // Configuration
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int CACHE_CLEANUP_HOURS = 2;

    // ===================== EMAIL ONLY METHODS =====================

    /**
     * Get user email by userId (with caching and Kafka communication)
     */
    public CompletableFuture<String> getUserEmail(UUID userId) {
        return getUserEmailWithDetails(userId, "EMAIL_NOTIFICATION")
                .thenApply(response -> response != null && "SUCCESS".equals(response.getStatus()) ?
                        response.getEmail() : null);
    }

    /**
     * Get user email with full details
     */
    public CompletableFuture<UserEmailResponse> getUserEmailWithDetails(UUID userId, String purpose) {
        // Check cache first
        CachedUserEmail cached = emailCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            log.debug("Retrieved user email from cache: {}", userId);
            return CompletableFuture.completedFuture(
                    UserEmailResponse.success("cached", userId, cached.getEmail())
                            .toBuilder()
                            .firstName(cached.getFirstName())
                            .lastName(cached.getLastName())
                            .emailVerified(cached.isEmailVerified())
                            .marketingOptIn(cached.isMarketingOptIn())
                            .build()
            );
        }

        // Send request to User Service via Kafka
        UserEmailRequest request = UserEmailRequest.create(userId, purpose);
        CompletableFuture<UserEmailResponse> future = new CompletableFuture<>();

        // Store the future for response handling
        pendingEmailRequests.put(request.getRequestId(), future);

        // Send Kafka message
        try {
            kafkaTemplate.send(TOPIC_USER_EMAIL_REQUEST, userId.toString(), request);
            log.debug("Sent user email request via Kafka: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send user email request via Kafka", e);
            pendingEmailRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
            return future;
        }

        // Set timeout
        CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> {
                    if (pendingEmailRequests.remove(request.getRequestId()) != null) {
                        log.warn("User email request timed out: {}", request.getRequestId());
                        future.complete(UserEmailResponse.error(request.getRequestId(), userId, "Request timeout"));
                    }
                });

        return future;
    }

    /**
     * Get multiple user emails (bulk request)
     */
    public CompletableFuture<Map<UUID, String>> getBulkUserEmails(List<UUID> userIds, String purpose) {
        if (userIds == null || userIds.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        // Check cache for some users
        Map<UUID, String> cachedResults = new HashMap<>();
        List<UUID> uncachedUserIds = new ArrayList<>();

        for (UUID userId : userIds) {
            CachedUserEmail cached = emailCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                cachedResults.put(userId, cached.getEmail());
            } else {
                uncachedUserIds.add(userId);
            }
        }

        // If all users are cached, return immediately
        if (uncachedUserIds.isEmpty()) {
            log.debug("All user emails retrieved from cache: {} users", cachedResults.size());
            return CompletableFuture.completedFuture(cachedResults);
        }

        // Send bulk request for uncached users
        BulkUserEmailRequest request = BulkUserEmailRequest.create(uncachedUserIds, purpose);
        CompletableFuture<BulkUserEmailResponse> future = new CompletableFuture<>();

        // Store the future for response handling
        pendingBulkEmailRequests.put(request.getRequestId(), future);

        // Send Kafka message
        try {
            kafkaTemplate.send(TOPIC_BULK_USER_EMAIL_REQUEST, "bulk-" + System.currentTimeMillis(), request);
            log.debug("Sent bulk user email request via Kafka: {} users", uncachedUserIds.size());
        } catch (Exception e) {
            log.error("Failed to send bulk user email request via Kafka", e);
            pendingBulkEmailRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
            return CompletableFuture.completedFuture(cachedResults);
        }

        // Set timeout
        CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> {
                    if (pendingBulkEmailRequests.remove(request.getRequestId()) != null) {
                        log.warn("Bulk user email request timed out: {}", request.getRequestId());
                        future.complete(BulkUserEmailResponse.create(request.getRequestId(), new ArrayList<>()));
                    }
                });

        // Combine cached and fetched results
        return future.thenApply(bulkResponse -> {
            Map<UUID, String> allResults = new HashMap<>(cachedResults);

            if (bulkResponse != null && bulkResponse.getUsers() != null) {
                for (UserEmailResponse userResponse : bulkResponse.getUsers()) {
                    if ("SUCCESS".equals(userResponse.getStatus()) && userResponse.getEmail() != null) {
                        allResults.put(userResponse.getUserId(), userResponse.getEmail());
                    }
                }
            }

            return allResults;
        });
    }

    // ===================== FULL USER INFO METHODS =====================

    /**
     * Get complete user information by userId
     */
    public CompletableFuture<UserInfoResponse> getUserInfo(UUID userId, String purpose) {
        return getUserInfo(userId, purpose, true, false);
    }

    /**
     * Get complete user information with options
     */
    public CompletableFuture<UserInfoResponse> getUserInfo(UUID userId, String purpose,
                                                           boolean includeAddresses, boolean includeRoles) {
        // Check cache first
        CachedUserInfo cached = userInfoCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            log.debug("Retrieved user info from cache: {}", userId);
            return CompletableFuture.completedFuture(cached.toUserInfoResponse("cached"));
        }

        // Send request to User Service via Kafka
        UserInfoRequest request = UserInfoRequest.create(userId, purpose, includeAddresses, includeRoles);
        CompletableFuture<UserInfoResponse> future = new CompletableFuture<>();

        // Store the future for response handling
        pendingUserInfoRequests.put(request.getRequestId(), future);

        // Send Kafka message
        try {
            kafkaTemplate.send(TOPIC_USER_INFO_REQUEST, userId.toString(), request);
            log.debug("Sent user info request via Kafka: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send user info request via Kafka", e);
            pendingUserInfoRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
            return future;
        }

        // Set timeout
        CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> {
                    if (pendingUserInfoRequests.remove(request.getRequestId()) != null) {
                        log.warn("User info request timed out: {}", request.getRequestId());
                        future.complete(UserInfoResponse.error(request.getRequestId(), userId, "Request timeout"));
                    }
                });

        return future;
    }

    /**
     * Get multiple user information (bulk request)
     */
    public CompletableFuture<Map<UUID, UserInfoResponse>> getBulkUserInfo(List<UUID> userIds, String purpose) {
        return getBulkUserInfo(userIds, purpose, true, false);
    }

    /**
     * Get multiple user information with options
     */
    public CompletableFuture<Map<UUID, UserInfoResponse>> getBulkUserInfo(List<UUID> userIds, String purpose,
                                                                          boolean includeAddresses, boolean includeRoles) {
        if (userIds == null || userIds.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        // Check cache for some users
        Map<UUID, UserInfoResponse> cachedResults = new HashMap<>();
        List<UUID> uncachedUserIds = new ArrayList<>();

        for (UUID userId : userIds) {
            CachedUserInfo cached = userInfoCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                cachedResults.put(userId, cached.toUserInfoResponse("cached"));
            } else {
                uncachedUserIds.add(userId);
            }
        }

        // If all users are cached, return immediately
        if (uncachedUserIds.isEmpty()) {
            log.debug("All user info retrieved from cache: {} users", cachedResults.size());
            return CompletableFuture.completedFuture(cachedResults);
        }

        // Send bulk request for uncached users
        BulkUserInfoRequest request = BulkUserInfoRequest.create(uncachedUserIds, purpose, includeAddresses, includeRoles);
        CompletableFuture<BulkUserInfoResponse> future = new CompletableFuture<>();

        // Store the future for response handling
        pendingBulkUserInfoRequests.put(request.getRequestId(), future);

        // Send Kafka message
        try {
            kafkaTemplate.send(TOPIC_BULK_USER_INFO_REQUEST, "bulk-" + System.currentTimeMillis(), request);
            log.debug("Sent bulk user info request via Kafka: {} users", uncachedUserIds.size());
        } catch (Exception e) {
            log.error("Failed to send bulk user info request via Kafka", e);
            pendingBulkUserInfoRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
            return CompletableFuture.completedFuture(cachedResults);
        }

        // Set timeout
        CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> {
                    if (pendingBulkUserInfoRequests.remove(request.getRequestId()) != null) {
                        log.warn("Bulk user info request timed out: {}", request.getRequestId());
                        future.complete(BulkUserInfoResponse.create(request.getRequestId(), new ArrayList<>()));
                    }
                });

        // Combine cached and fetched results
        return future.thenApply(bulkResponse -> {
            Map<UUID, UserInfoResponse> allResults = new HashMap<>(cachedResults);

            if (bulkResponse != null && bulkResponse.getUsers() != null) {
                for (UserInfoResponse userResponse : bulkResponse.getUsers()) {
                    if ("SUCCESS".equals(userResponse.getStatus_response())) {
                        allResults.put(UUID.fromString(String.valueOf(userResponse.getUserId())), userResponse);
                    }
                }
            }

            return allResults;
        });
    }

    // ===================== KAFKA LISTENERS (UPDATED FOR STRING PAYLOADS) =====================

    /**
     * Listen for user email responses from User Service
     * UPDATED: Now accepts String payload and manually deserializes
     */
    @KafkaListener(topics = TOPIC_USER_EMAIL_RESPONSE, groupId = "notification-service-user-email")
    public void handleUserEmailResponse(String payload) {
        try {
            // Manual deserialization from JSON string
            UserEmailResponse response = objectMapper.readValue(payload, UserEmailResponse.class);
            log.debug("Received user email response: {}", response.getRequestId());

            CompletableFuture<UserEmailResponse> future = pendingEmailRequests.remove(response.getRequestId());
            if (future != null) {
                // Cache successful responses
                if ("SUCCESS".equals(response.getStatus()) && response.getEmail() != null) {
                    CachedUserEmail cached = CachedUserEmail.fromResponse(response);
                    emailCache.put(response.getUserId(), cached);
                    log.debug("Cached user email: {}", response.getUserId());
                }

                future.complete(response);
            } else {
                log.warn("Received response for unknown request: {}", response.getRequestId());
            }
        } catch (Exception e) {
            log.error("Error processing user email response: {}", payload, e);
        }
    }

    /**
     * Listen for bulk user email responses from User Service
     * UPDATED: Now accepts String payload and manually deserializes
     */
    @KafkaListener(topics = TOPIC_BULK_USER_EMAIL_RESPONSE, groupId = "notification-service-bulk-user-email")
    public void handleBulkUserEmailResponse(String payload) {
        try {
            // Manual deserialization from JSON string
            BulkUserEmailResponse response = objectMapper.readValue(payload, BulkUserEmailResponse.class);
            log.debug("Received bulk user email response: {} users", response.getTotalFound());

            CompletableFuture<BulkUserEmailResponse> future = pendingBulkEmailRequests.remove(response.getRequestId());
            if (future != null) {
                // Cache successful responses
                if (response.getUsers() != null) {
                    for (UserEmailResponse userResponse : response.getUsers()) {
                        if ("SUCCESS".equals(userResponse.getStatus()) && userResponse.getEmail() != null) {
                            CachedUserEmail cached = CachedUserEmail.fromResponse(userResponse);
                            emailCache.put(userResponse.getUserId(), cached);
                        }
                    }
                    log.debug("Cached {} user emails from bulk response",
                            response.getUsers().stream().mapToInt(u -> "SUCCESS".equals(u.getStatus()) ? 1 : 0).sum());
                }

                future.complete(response);
            } else {
                log.warn("Received bulk response for unknown request: {}", response.getRequestId());
            }
        } catch (Exception e) {
            log.error("Error processing bulk user email response: {}", payload, e);
        }
    }

    /**
     * Listen for user info responses from User Service
     * UPDATED: Now accepts String payload and manually deserializes
     */
    @KafkaListener(topics = TOPIC_USER_INFO_RESPONSE, groupId = "notification-service-user-info")
    public void handleUserInfoResponse(String payload) {
        try {
            // Manual deserialization from JSON string
            UserInfoResponse response = objectMapper.readValue(payload, UserInfoResponse.class);
            log.debug("Received user info response: {}", response.getRequestId());

            CompletableFuture<UserInfoResponse> future = pendingUserInfoRequests.remove(response.getRequestId());
            if (future != null) {
                // Cache successful responses
                if ("SUCCESS".equals(response.getStatus_response())) {
                    CachedUserInfo cached = CachedUserInfo.fromResponse(response);
                    userInfoCache.put(UUID.fromString(String.valueOf(response.getUserId())), cached);
                    log.debug("Cached user info: {}", response.getUserId());
                }

                future.complete(response);
            } else {
                log.warn("Received response for unknown request: {}", response.getRequestId());
            }
        } catch (Exception e) {
            log.error("Error processing user info response: {}", payload, e);
        }
    }

    /**
     * Listen for bulk user info responses from User Service
     * UPDATED: Now accepts String payload and manually deserializes
     */
    @KafkaListener(topics = TOPIC_BULK_USER_INFO_RESPONSE, groupId = "notification-service-bulk-user-info")
    public void handleBulkUserInfoResponse(String payload) {
        try {
            // Manual deserialization from JSON string
            BulkUserInfoResponse response = objectMapper.readValue(payload, BulkUserInfoResponse.class);
            log.debug("Received bulk user info response: {} users", response.getTotalFound());

            CompletableFuture<BulkUserInfoResponse> future = pendingBulkUserInfoRequests.remove(response.getRequestId());
            if (future != null) {
                // Cache successful responses
                if (response.getUsers() != null) {
                    for (UserInfoResponse userResponse : response.getUsers()) {
                        if ("SUCCESS".equals(userResponse.getStatus_response())) {
                            CachedUserInfo cached = CachedUserInfo.fromResponse(userResponse);
                            userInfoCache.put(UUID.fromString(String.valueOf(userResponse.getUserId())), cached);
                        }
                    }
                    log.debug("Cached {} user info from bulk response",
                            response.getUsers().stream().mapToInt(u -> "SUCCESS".equals(u.getStatus_response()) ? 1 : 0).sum());
                }

                future.complete(response);
            } else {
                log.warn("Received bulk response for unknown request: {}", response.getRequestId());
            }
        } catch (Exception e) {
            log.error("Error processing bulk user info response: {}", payload, e);
        }
    }

    // ===================== FALLBACK METHODS =====================

    /**
     * Get user email with fallback to demo email (for testing without User Service)
     */
    public CompletableFuture<String> getUserEmailWithFallback(UUID userId) {
        return getUserEmail(userId)
                .thenApply(email -> {
                    if (email != null && !email.isEmpty()) {
                        return email;
                    }
                    // Fallback for testing when User Service is not available
                    String fallbackEmail = generateFallbackEmail(userId);
                    log.debug("Using fallback email for user {}: {}", userId, fallbackEmail);
                    return fallbackEmail;
                });
    }

    /**
     * Get multiple user emails with fallback
     */
    public CompletableFuture<Map<UUID, String>> getBulkUserEmailsWithFallback(List<UUID> userIds, String purpose) {
        return getBulkUserEmails(userIds, purpose)
                .thenApply(emailMap -> {
                    Map<UUID, String> result = new HashMap<>(emailMap);

                    // Add fallback emails for users not found
                    for (UUID userId : userIds) {
                        if (!result.containsKey(userId) || result.get(userId) == null) {
                            String fallbackEmail = generateFallbackEmail(userId);
                            result.put(userId, fallbackEmail);
                            log.debug("Using fallback email for user {}: {}", userId, fallbackEmail);
                        }
                    }

                    return result;
                });
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Generate fallback email for testing (when User Service is not available)
     */
    private String generateFallbackEmail(UUID userId) {
        // Generate a consistent email based on user ID for testing
        String shortId = userId.toString().substring(0, 8);
        return String.format("user-%s@demo.ecommerce.com", shortId);
    }

    /**
     * Clear expired cache entries
     */
    public void cleanupExpiredCache() {
        int initialEmailCacheSize = emailCache.size();
        int initialInfoCacheSize = userInfoCache.size();

        emailCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        userInfoCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        int removedEmailCount = initialEmailCacheSize - emailCache.size();
        int removedInfoCount = initialInfoCacheSize - userInfoCache.size();

        if (removedEmailCount > 0 || removedInfoCount > 0) {
            log.debug("Cleaned up {} email cache entries and {} info cache entries", removedEmailCount, removedInfoCount);
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        int totalEmailCached = emailCache.size();
        int expiredEmailCount = (int) emailCache.values().stream().filter(CachedUserEmail::isExpired).count();

        int totalInfoCached = userInfoCache.size();
        int expiredInfoCount = (int) userInfoCache.values().stream().filter(CachedUserInfo::isExpired).count();

        return Map.of(
                "totalEmailCached", totalEmailCached,
                "validEmailCached", totalEmailCached - expiredEmailCount,
                "expiredEmailCached", expiredEmailCount,
                "totalInfoCached", totalInfoCached,
                "validInfoCached", totalInfoCached - expiredInfoCount,
                "expiredInfoCached", expiredInfoCount,
                "pendingEmailRequests", pendingEmailRequests.size(),
                "pendingBulkEmailRequests", pendingBulkEmailRequests.size(),
                "pendingInfoRequests", pendingUserInfoRequests.size(),
                "pendingBulkInfoRequests", pendingBulkUserInfoRequests.size()
        );
    }

    /**
     * Manually add user info to cache (for testing)
     */
    public void cacheUserInfo(UUID userId, String email, String username) {
        CachedUserEmail emailCached = CachedUserEmail.builder()
                .userId(userId)
                .email(email)
                .emailVerified(true)
                .marketingOptIn(true)
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        CachedUserInfo infoCached = CachedUserInfo.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .emailVerified(true)
                .marketingOptIn(true)
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        emailCache.put(userId, emailCached);
        userInfoCache.put(userId, infoCached);
        log.debug("Manually cached user info: {} -> {} ({})", userId, email, username);
    }

    /**
     * Clear all cache (for testing)
     */
    public void clearCache() {
        emailCache.clear();
        userInfoCache.clear();
        log.debug("Cleared all cached user data");
    }

    /**
     * Check if User Service is responsive
     */
    public CompletableFuture<Boolean> isUserServiceAvailable() {
        UUID testUserId = UUID.randomUUID();
        return getUserEmailWithDetails(testUserId, "HEALTH_CHECK")
                .thenApply(response -> response != null)
                .exceptionally(ex -> false)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ex -> false);
    }
}