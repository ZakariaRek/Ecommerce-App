package com.Ecommerce.Gateway_Service.Security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomRateLimitFilterFactoryTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private GatewayFilterChain chain;

    private CustomRateLimitFilterFactory filterFactory;
    private CustomRateLimitFilterFactory.Config config;

    @BeforeEach
    void setUp() {
        filterFactory = new CustomRateLimitFilterFactory(redisTemplate);
        config = new CustomRateLimitFilterFactory.Config();
        config.setLimit(5);
        config.setWindowSizeInSeconds(60);
        config.setKeyType(CustomRateLimitFilterFactory.KeyType.IP);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(request.getPath()).thenReturn((RequestPath) PathContainer.parsePath("/api/test"));
    }

    @Test
    void shouldAllowRequestWithinRateLimit() {
        // Given
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filterFactory.apply(config).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldRejectRequestExceedingRateLimit() {
        // Given
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(6L)); // Exceeds limit of 5
        when(response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS)).thenReturn(true);
        when(response.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        when(response.bufferFactory()).thenReturn(new org.springframework.core.io.buffer.DefaultDataBufferFactory());
        when(response.writeWith(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filterFactory.apply(config).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}