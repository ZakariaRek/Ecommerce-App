package com.Ecommerce.Gateway_Service.Security;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;

@Component
public class TokenBucketRateLimitFilterFactory extends AbstractGatewayFilterFactory<TokenBucketRateLimitFilterFactory.Config> {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    public TokenBucketRateLimitFilterFactory(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;

        // Lua script for atomic token bucket operations
        String script = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local tokens = tonumber(ARGV[2])
            local interval = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])
            local now = tonumber(ARGV[5])
            
            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local current_tokens = tonumber(bucket[1]) or capacity
            local last_refill = tonumber(bucket[2]) or now
            
            -- Calculate tokens to add based on time elapsed
            local time_passed = now - last_refill
            local tokens_to_add = math.floor(time_passed / interval * tokens)
            current_tokens = math.min(capacity, current_tokens + tokens_to_add)
            
            if current_tokens >= requested then
                current_tokens = current_tokens - requested
                redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill', now)
                redis.call('EXPIRE', key, interval * 2)
                return 1
            else
                redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill', now)
                redis.call('EXPIRE', key, interval * 2)
                return 0
            end
            """;

        this.tokenBucketScript = RedisScript.of(script, Long.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String key = "token_bucket:" + getClientIP(exchange.getRequest()) + ":" +
                    sanitizePath(exchange.getRequest().getPath().value());
            long now = Instant.now().getEpochSecond();

            // Fix: Use .next() to convert Flux<Long> to Mono<Long>
            return redisTemplate.execute(tokenBucketScript,
                            Arrays.asList(key),
                            String.valueOf(config.getBucketCapacity()),
                            String.valueOf(config.getRefillTokens()),
                            String.valueOf(config.getRefillIntervalSeconds()),
                            String.valueOf(config.getRequestedTokens()),
                            String.valueOf(now))
                    .cast(Long.class)
                    .next() // Convert Flux<Long> to Mono<Long>
                    .flatMap(allowed -> {
                        if (allowed == 1) {
                            // Add headers showing bucket status
                            exchange.getResponse().getHeaders().add("X-RateLimit-Type", "TokenBucket");
                            exchange.getResponse().getHeaders().add("X-RateLimit-Capacity", String.valueOf(config.getBucketCapacity()));
                            exchange.getResponse().getHeaders().add("X-RateLimit-Refill-Rate",
                                    config.getRefillTokens() + "/" + config.getRefillIntervalSeconds() + "s");
                            return chain.filter(exchange);
                        } else {
                            return handleTokenBucketExceeded(exchange, config);
                        }
                    })
                    .onErrorResume(throwable -> {
                        // If Redis fails, allow the request (fail-open policy)
                        System.err.println("Token bucket Redis error: " + throwable.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    private Mono<Void> handleTokenBucketExceeded(org.springframework.web.server.ServerWebExchange exchange, Config config) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        exchange.getResponse().getHeaders().add("X-RateLimit-Type", "TokenBucket");
        exchange.getResponse().getHeaders().add("X-RateLimit-Capacity", String.valueOf(config.getBucketCapacity()));
        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(config.getRefillIntervalSeconds()));

        String body = String.format(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Token bucket exhausted. Capacity: %d tokens\",\"refillRate\":\"%d tokens per %d seconds\",\"timestamp\":\"%s\"}",
                config.getBucketCapacity(),
                config.getRefillTokens(),
                config.getRefillIntervalSeconds(),
                Instant.now().toString()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private String sanitizePath(String path) {
        // Remove dynamic path parameters for consistent rate limiting
        return path.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}")
                .replaceAll("/\\d+", "/{id}");
    }

    public static class Config {
        private int bucketCapacity = 10;
        private int refillTokens = 1;
        private int refillIntervalSeconds = 1;
        private int requestedTokens = 1;

        // Getters and setters
        public int getBucketCapacity() { return bucketCapacity; }
        public void setBucketCapacity(int bucketCapacity) { this.bucketCapacity = bucketCapacity; }

        public int getRefillTokens() { return refillTokens; }
        public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }

        public int getRefillIntervalSeconds() { return refillIntervalSeconds; }
        public void setRefillIntervalSeconds(int refillIntervalSeconds) { this.refillIntervalSeconds = refillIntervalSeconds; }

        public int getRequestedTokens() { return requestedTokens; }
        public void setRequestedTokens(int requestedTokens) { this.requestedTokens = requestedTokens; }
    }
}