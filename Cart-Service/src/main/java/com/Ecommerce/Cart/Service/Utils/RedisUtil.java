package com.Ecommerce.Cart.Service.Utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for direct Redis operations
 * Useful for operations that go beyond the @Cacheable annotations
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Store a value in Redis with a specific expiration time
     */
    public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * Get a value from Redis
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Check if a key exists in Redis
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Delete a key from Redis
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Get the TTL for a key
     */
    public long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    /**
     * Update the expiration time for a key
     */
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
    }

    /**
     * Get Redis keys matching a pattern
     */
    public Set<String> getKeysWithPattern(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * Get the type of a key
     */
    public DataType getType(String key) {
        return redisTemplate.type(key);
    }

    /**
     * Delete multiple keys
     */
    public Long deleteKeys(Set<String> keys) {
        return redisTemplate.delete(keys);
    }
}