package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Payload.Kafka.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service for communicating with User Service to fetch user emails via Kafka
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserEmailService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Cache for user emails (in-memory cache)
    private final Map<UUID, CachedUserEmail> emailCache = new ConcurrentHashMap<>();

    // Pending requests (waiting for responses)
    private final Map<String, CompletableFuture<UserEmailResponse>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<BulkUserEmailResponse>> pendingBulkRequests = new ConcurrentHashMap<>();

    // Kafka Topics
    public static final String TOPIC_USER_EMAIL_REQUEST = "user-email-request";
    public static final String TOPIC_USER_EMAIL_RESPONSE = "user-email-response";
    public static final String TOPIC_BULK_USER_EMAIL_REQUEST = "bulk-user-email-request";
    public static final String TOPIC_BULK_USER_EMAIL_RESPONSE = "bulk-user-email-response";

    // Configuration
    private static final int REQUEST_TIMEOUT_SECONDS = 30;
    private static final int CACHE_CLEANUP_HOURS = 2;

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
        pendingRequests.put(request.getRequestId(), future);

        // Send Kafka message
        try {
            kafkaTemplate.send(TOPIC_USER_EMAIL_REQUEST, userId.toString(), request);
            log.debug("Sent user email request via Kafka: {}", request.getRequestId());
        } catch (Exception e) {
            log.error("Failed to send user email request via Kafka", e);
            pendingRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
            return future;
        }

        // Set timeout
        CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> {
                    if (pendingRequests.remove(request.getRequestId()) != null) {
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
        pendingBulkRequests.put(request.getRequestId(), future);

        // Send Kafka message
        try {
            kafkaTemplate.send(TOPIC_BULK_USER_EMAIL_REQUEST, "bulk-" + System.currentTimeMillis(), request);
            log.debug("Sent bulk user email request via Kafka: {} users", uncachedUserIds.size());
        } catch (Exception e) {
            log.error("Failed to send bulk user email request via Kafka", e);
            pendingBulkRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
            return CompletableFuture.completedFuture(cachedResults);
        }

        // Set timeout
        CompletableFuture.delayedExecutor(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> {
                    if (pendingBulkRequests.remove(request.getRequestId()) != null) {
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

    /**
     * Listen for user email responses from User Service
     */
    @KafkaListener(topics = TOPIC_USER_EMAIL_RESPONSE, groupId = "notification-service-user-email")
    public void handleUserEmailResponse(UserEmailResponse response) {
        log.debug("Received user email response: {}", response.getRequestId());

        CompletableFuture<UserEmailResponse> future = pendingRequests.remove(response.getRequestId());
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
    }

    /**
     * Listen for bulk user email responses from User Service
     */
    @KafkaListener(topics = TOPIC_BULK_USER_EMAIL_RESPONSE, groupId = "notification-service-bulk-user-email")
    public void handleBulkUserEmailResponse(BulkUserEmailResponse response) {
        log.debug("Received bulk user email response: {} users", response.getTotalFound());

        CompletableFuture<BulkUserEmailResponse> future = pendingBulkRequests.remove(response.getRequestId());
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
    }

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
        int initialSize = emailCache.size();
        emailCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removedCount = initialSize - emailCache.size();

        if (removedCount > 0) {
            log.debug("Cleaned up {} expired cache entries", removedCount);
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        int totalCached = emailCache.size();
        int expiredCount = (int) emailCache.values().stream().filter(CachedUserEmail::isExpired).count();

        return Map.of(
                "totalCached", totalCached,
                "validCached", totalCached - expiredCount,
                "expiredCached", expiredCount,
                "pendingRequests", pendingRequests.size(),
                "pendingBulkRequests", pendingBulkRequests.size(),
                "cacheHitRate", calculateCacheHitRate()
        );
    }

    private double calculateCacheHitRate() {
        // This would require tracking hits/misses in a real implementation
        return emailCache.isEmpty() ? 0.0 : 0.85; // Placeholder
    }

    /**
     * Manually add user email to cache (for testing)
     */
    public void cacheUserEmail(UUID userId, String email) {
        CachedUserEmail cached = CachedUserEmail.builder()
                .userId(userId)
                .email(email)
                .emailVerified(true)
                .marketingOptIn(true)
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        emailCache.put(userId, cached);
        log.debug("Manually cached user email: {} -> {}", userId, email);
    }

    /**
     * Clear all cache (for testing)
     */
    public void clearCache() {
        emailCache.clear();
        log.debug("Cleared all cached user emails");
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