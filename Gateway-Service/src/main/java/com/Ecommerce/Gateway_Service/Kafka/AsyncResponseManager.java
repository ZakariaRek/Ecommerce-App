package com.Ecommerce.Gateway_Service.Kafka;

import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

@Component
@Slf4j
public class AsyncResponseManager {

    private final Map<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public <T> Mono<T> waitForResponse(String correlationId, Duration timeout, Class<T> responseType) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        // Set timeout
        scheduler.schedule(() -> {
            CompletableFuture<Object> removed = pendingRequests.remove(correlationId);
            if (removed != null && !removed.isDone()) {
                removed.completeExceptionally(new RuntimeException("Request timeout for correlationId: " + correlationId));
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);

        return Mono.fromFuture(future)
                .cast(responseType)
                .timeout(timeout)
                .doFinally(signal -> pendingRequests.remove(correlationId));
    }

    public void completeRequest(String correlationId, Object response) {
        CompletableFuture<Object> future = pendingRequests.remove(correlationId);

        if (future != null) {
            future.complete(response);
            log.debug("Completed request for correlationId: {}", correlationId);
        } else {
            log.warn("No pending request found for correlationId: {}", correlationId);
        }
    }

    public void completeRequestExceptionally(String correlationId, Throwable throwable) {
        CompletableFuture<Object> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.completeExceptionally(throwable);
            log.error("Completed request exceptionally for correlationId: {}", correlationId, throwable);
        }
    }
}