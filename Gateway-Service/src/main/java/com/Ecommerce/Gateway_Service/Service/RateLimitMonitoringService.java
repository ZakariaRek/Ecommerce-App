package com.Ecommerce.Gateway_Service.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RateLimitMonitoringService {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    public Mono<RateLimitInfo> getRateLimitInfo(String key) {
        return redisTemplate.opsForValue().get(key)
                .zipWith(redisTemplate.getExpire(key))
                .map(tuple -> {
                    Long currentCount = tuple.getT1() != null ? Long.valueOf(tuple.getT1()) : 0L;
                    Duration ttl = tuple.getT2();
                    return new RateLimitInfo(key, currentCount, ttl.getSeconds());
                })
                .defaultIfEmpty(new RateLimitInfo(key, 0L, 0L));
    }

    public Mono<Void> resetRateLimit(String key) {
        return redisTemplate.delete(key).then();
    }

    public static class RateLimitInfo {
        private final String key;
        private final Long currentCount;
        private final Long ttlSeconds;

        public RateLimitInfo(String key, Long currentCount, Long ttlSeconds) {
            this.key = key;
            this.currentCount = currentCount;
            this.ttlSeconds = ttlSeconds;
        }

        public String getKey() { return key; }
        public Long getCurrentCount() { return currentCount; }
        public Long getTtlSeconds() { return ttlSeconds; }
    }
}