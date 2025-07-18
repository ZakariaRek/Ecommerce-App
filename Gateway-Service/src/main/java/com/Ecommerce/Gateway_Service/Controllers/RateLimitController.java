package com.Ecommerce.Gateway_Service.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/gateway/rate-limiting")
@Tag(name = "Rate Limiting Management", description = "Rate limiting monitoring and management endpoints")
public class RateLimitController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Operation(
            summary = "Get rate limiting configuration",
            description = "Returns the current rate limiting configuration for all endpoints"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved rate limiting configuration",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RateLimitConfigResponse.class)))
    })
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getRateLimitConfig() {
        Map<String, Object> config = new HashMap<>();

        List<RateLimitEndpointConfig> endpointConfigs = List.of(
                new RateLimitEndpointConfig("auth", "/api/users/auth/**", 5, 60, "IP", "Strict limit for authentication to prevent brute force"),
                new RateLimitEndpointConfig("payment", "/api/payments/**", 3, 300, "USER", "Very restrictive for payment operations"),
                new RateLimitEndpointConfig("admin", "/api/users/**", 20, 60, "USER", "Moderate limit for admin operations"),
                new RateLimitEndpointConfig("public-read", "/api/products/**", 200, 60, "IP", "Higher limit for public read operations"),
                new RateLimitEndpointConfig("user-operations", "Various user endpoints", 50, 60, "USER", "Standard limit for authenticated user operations"),
                new RateLimitEndpointConfig("cart", "/api/cart/**", 50, 60, "USER", "High limits for frequent operations"),

                new RateLimitEndpointConfig("order", "/api/orders/**", 30, 60, "USER", "Moderate limits for order operations"),
                new RateLimitEndpointConfig("loyalty", "/api/loyalty/**", 40, 60, "USER", "Moderate limits for loyalty operations"),
                new RateLimitEndpointConfig("notification", "/api/notifications/**", 30, 60, "USER", "Moderate limits for notification operations"),
                new RateLimitEndpointConfig("shipping", "/api/shipping/**", 25, 60, "USER", "Moderate limits for shipping operations")
        );

        config.put("timestamp", LocalDateTime.now());
        config.put("totalEndpoints", endpointConfigs.size());
        config.put("endpoints", endpointConfigs);

        return ResponseEntity.ok(config);
    }

    @Operation(
            summary = "Get current rate limit status for user/IP",
            description = "Returns the current rate limit status for a specific user or IP address"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved rate limit status",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RateLimitStatusResponse.class))),
            @ApiResponse(responseCode = "404", description = "No rate limit data found for the specified key")
    })
    @GetMapping("/status/{keyType}/{identifier}")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
            @Parameter(description = "Type of key (IP or USER)", required = true)
            @PathVariable String keyType,
            @Parameter(description = "IP address or user identifier", required = true)
            @PathVariable String identifier) {

        Map<String, Object> status = new HashMap<>();

        // Check different rate limit buckets for this identifier
        String[] endpoints = {"auth", "payment", "admin", "public-read", "user-operations", "cart", "order", "loyalty", "notification", "shipping"};

        Map<String, RateLimitStatus> endpointStatuses = new HashMap<>();

        for (String endpoint : endpoints) {
            String redisKey = "rate_limit:" + endpoint + ":" + keyType.toLowerCase() + ":" + identifier;
            String currentCount = redisTemplate.opsForValue().get(redisKey);
            Long ttl = redisTemplate.getExpire(redisKey);

            if (currentCount != null) {
                RateLimitStatus rateLimitStatus = new RateLimitStatus(
                        endpoint,
                        Integer.parseInt(currentCount),
                        getRateLimitForEndpoint(endpoint),
                        ttl != null ? ttl.intValue() : 0,
                        Integer.parseInt(currentCount) >= getRateLimitForEndpoint(endpoint)
                );
                endpointStatuses.put(endpoint, rateLimitStatus);
            }
        }

        status.put("identifier", identifier);
        status.put("keyType", keyType);
        status.put("timestamp", LocalDateTime.now());
        status.put("endpointStatuses", endpointStatuses);

        return ResponseEntity.ok(status);
    }

    @Operation(
            summary = "Reset rate limit for user/IP",
            description = "Resets the rate limit counters for a specific user or IP address",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rate limit reset successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/reset/{keyType}/{identifier}")
    public ResponseEntity<Map<String, String>> resetRateLimit(
            @Parameter(description = "Type of key (IP or USER)", required = true)
            @PathVariable String keyType,
            @Parameter(description = "IP address or user identifier", required = true)
            @PathVariable String identifier) {

        // Reset rate limits for all endpoints for this identifier
        String[] endpoints = {"auth", "payment", "admin", "public-read", "user-operations", "cart", "order", "loyalty", "notification", "shipping"};

        int resetCount = 0;
        for (String endpoint : endpoints) {
            String redisKey = "rate_limit:" + endpoint + ":" + keyType.toLowerCase() + ":" + identifier;
            if (redisTemplate.delete(redisKey)) {
                resetCount++;
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Rate limits reset for " + identifier);
        response.put("keyType", keyType);
        response.put("identifier", identifier);
        response.put("endpointsReset", String.valueOf(resetCount));
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get rate limiting statistics",
            description = "Returns overall rate limiting statistics and metrics"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved rate limiting statistics",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RateLimitStatsResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRateLimitStats() {
        Map<String, Object> stats = new HashMap<>();

        // Get all rate limit keys from Redis
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");

        Map<String, Integer> endpointCounts = new HashMap<>();
        Map<String, Integer> keyTypeCounts = new HashMap<>();

        if (rateLimitKeys != null) {
            for (String key : rateLimitKeys) {
                String[] parts = key.split(":");
                if (parts.length >= 4) {
                    String endpoint = parts[1];
                    String keyType = parts[2];

                    endpointCounts.put(endpoint, endpointCounts.getOrDefault(endpoint, 0) + 1);
                    keyTypeCounts.put(keyType, keyTypeCounts.getOrDefault(keyType, 0) + 1);
                }
            }
        }

        stats.put("timestamp", LocalDateTime.now());
        stats.put("totalActiveRateLimits", rateLimitKeys != null ? rateLimitKeys.size() : 0);
        stats.put("endpointBreakdown", endpointCounts);
        stats.put("keyTypeBreakdown", keyTypeCounts);

        return ResponseEntity.ok(stats);
    }

    private int getRateLimitForEndpoint(String endpoint) {
        switch (endpoint) {
            case "auth": return 5;
            case "payment": return 3;
            case "admin": return 20;
            case "public-read": return 200;
            case "user-operations": return 50;
            case "cart": return 50;
            case "order": return 30;
            case "loyalty": return 40;
            case "notification": return 30;
            case "shipping": return 25;
            default: return 100;
        }
    }

    // DTOs for API responses
    public static class RateLimitEndpointConfig {
        private String name;
        private String path;
        private int limit;
        private int windowSeconds;
        private String keyType;
        private String description;

        public RateLimitEndpointConfig(String name, String path, int limit, int windowSeconds, String keyType, String description) {
            this.name = name;
            this.path = path;
            this.limit = limit;
            this.windowSeconds = windowSeconds;
            this.keyType = keyType;
            this.description = description;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

        public String getKeyType() { return keyType; }
        public void setKeyType(String keyType) { this.keyType = keyType; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class RateLimitStatus {
        private String endpoint;
        private int currentCount;
        private int limit;
        private int remainingTtl;
        private boolean isLimited;

        public RateLimitStatus(String endpoint, int currentCount, int limit, int remainingTtl, boolean isLimited) {
            this.endpoint = endpoint;
            this.currentCount = currentCount;
            this.limit = limit;
            this.remainingTtl = remainingTtl;
            this.isLimited = isLimited;
        }

        // Getters and setters
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public int getCurrentCount() { return currentCount; }
        public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getRemainingTtl() { return remainingTtl; }
        public void setRemainingTtl(int remainingTtl) { this.remainingTtl = remainingTtl; }

        public boolean isLimited() { return isLimited; }
        public void setLimited(boolean limited) { isLimited = limited; }
    }

    // Schema classes for Swagger documentation
    public static class RateLimitConfigResponse {
        public LocalDateTime timestamp;
        public int totalEndpoints;
        public List<RateLimitEndpointConfig> endpoints;
    }

    public static class RateLimitStatusResponse {
        public String identifier;
        public String keyType;
        public LocalDateTime timestamp;
        public Map<String, RateLimitStatus> endpointStatuses;
    }

    public static class RateLimitStatsResponse {
        public LocalDateTime timestamp;
        public int totalActiveRateLimits;
        public Map<String, Integer> endpointBreakdown;
        public Map<String, Integer> keyTypeBreakdown;
    }
}