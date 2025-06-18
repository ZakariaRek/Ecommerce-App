//package com.Ecommerce.Gateway_Service.Security;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.data.redis.core.ReactiveRedisTemplate;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.util.List;
//
//@Component
//public class CustomRateLimitFilterFactory extends AbstractGatewayFilterFactory<CustomRateLimitFilterFactory.Config> {
//
//    @Autowired
//    private ReactiveStringRedisTemplate redisTemplate;
//
//    public CustomRateLimitFilterFactory() {
//        super(Config.class);
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//        return (exchange, chain) -> {
//            ServerHttpRequest request = exchange.getRequest();
//
//            // Create unique key for rate limiting
//            String key = createRateLimitKey(request, config);
//
//            return checkRateLimit(key, config)
//                    .flatMap(allowed -> {
//                        if (allowed) {
//                            // Add rate limit headers
//                            addRateLimitHeaders(exchange, config);
//                            return chain.filter(exchange);
//                        } else {
//                            return handleRateLimitExceeded(exchange, config);
//                        }
//                    });
//        };
//    }
//
//    private String createRateLimitKey(ServerHttpRequest request, Config config) {
//        String identifier;
//
//        switch (config.getKeyType()) {
//            case IP:
//                identifier = getClientIP(request);
//                break;
//            case USER:
//                identifier = getUserId(request);
//                break;
//            case API_KEY:
//                identifier = getApiKey(request);
//                break;
//            default:
//                identifier = getClientIP(request);
//        }
//
//        return String.format("rate_limit:%s:%s:%s",
//                config.getKeyType().name().toLowerCase(),
//                identifier,
//                request.getPath().value()
//        );
//    }
//
//    private String getClientIP(ServerHttpRequest request) {
//        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
//        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
//            return xForwardedFor.split(",")[0].trim();
//        }
//
//        String xRealIP = request.getHeaders().getFirst("X-Real-IP");
//        if (xRealIP != null && !xRealIP.isEmpty()) {
//            return xRealIP;
//        }
//
//        return request.getRemoteAddress() != null ?
//                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
//    }
//
//    private String getUserId(ServerHttpRequest request) {
//        List<String> userHeaders = request.getHeaders().get("X-User-Id");
//        return userHeaders != null && !userHeaders.isEmpty() ? userHeaders.get(0) : getClientIP(request);
//    }
//
//    private String getApiKey(ServerHttpRequest request) {
//        List<String> apiKeyHeaders = request.getHeaders().get("X-API-Key");
//        return apiKeyHeaders != null && !apiKeyHeaders.isEmpty() ? apiKeyHeaders.get(0) : getClientIP(request);
//    }
//
//    private Mono<Boolean> checkRateLimit(String key, Config config) {
//        return redisTemplate.opsForValue()
//                .increment(key)
//                .flatMap(count -> {
//                    if (count == 1) {
//                        // First request, set expiration
//                        return redisTemplate.expire(key, Duration.ofSeconds(config.getWindowSizeInSeconds()))
//                                .map(expired -> count <= config.getLimit());
//                    } else {
//                        // Check if limit exceeded
//                        return Mono.just(count <= config.getLimit());
//                    }
//                })
//                .onErrorReturn(false); // If Redis fails, deny the request
//    }
//
//    private void addRateLimitHeaders(ServerWebExchange exchange, Config config) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getLimit()));
//        response.getHeaders().add("X-RateLimit-Window", String.valueOf(config.getWindowSizeInSeconds()));
//        response.getHeaders().add("X-RateLimit-Policy", config.getKeyType().name());
//    }
//
//    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, Config config) {
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
//        response.getHeaders().add("Content-Type", "application/json");
//        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getLimit()));
//        response.getHeaders().add("X-RateLimit-Window", String.valueOf(config.getWindowSizeInSeconds()));
//        response.getHeaders().add("Retry-After", String.valueOf(config.getWindowSizeInSeconds()));
//
//        String body = String.format(
//                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Limit: %d requests per %d seconds\",\"retryAfter\":%d}",
//                config.getLimit(),
//                config.getWindowSizeInSeconds(),
//                config.getWindowSizeInSeconds()
//        );
//
//        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
//    }
//
//    public static class Config {
//        private int limit = 100;
//        private int windowSizeInSeconds = 60;
//        private KeyType keyType = KeyType.IP;
//
//        public int getLimit() { return limit; }
//        public void setLimit(int limit) { this.limit = limit; }
//
//        public int getWindowSizeInSeconds() { return windowSizeInSeconds; }
//        public void setWindowSizeInSeconds(int windowSizeInSeconds) { this.windowSizeInSeconds = windowSizeInSeconds; }
//
//        public KeyType getKeyType() { return keyType; }
//        public void setKeyType(KeyType keyType) { this.keyType = keyType; }
//    }
//
//    public enum KeyType {
//        IP, USER, API_KEY
//    }
//}
