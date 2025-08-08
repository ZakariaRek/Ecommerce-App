package com.Ecommerce.Gateway_Service.Async;

import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

class AsyncResponseTimeoutTest {

    private AsyncResponseManager asyncResponseManager;

    @BeforeEach
    void setUp() {
        asyncResponseManager = new AsyncResponseManager();
    }

    @Test
    void shouldTimeoutWhenNoResponseReceived() {
        // Given
        String correlationId = "test-correlation-id";
        Duration timeout = Duration.ofMillis(100);

        // When
        Mono<String> result = asyncResponseManager.waitForResponse(
                correlationId, timeout, String.class
        );

        // Then
        StepVerifier.create(result)
                .expectError(TimeoutException.class)
                .verify();
    }

    @Test
    void shouldCompleteWhenResponseReceivedInTime() {
        // Given
        String correlationId = "test-correlation-id";
        String expectedResponse = "test-response";
        Duration timeout = Duration.ofSeconds(1);

        // When
        Mono<String> result = asyncResponseManager.waitForResponse(
                correlationId, timeout, String.class
        );

        // Simulate response after delay
        Mono.delay(Duration.ofMillis(50))
                .doOnNext(ignore -> asyncResponseManager.completeRequest(correlationId, expectedResponse))
                .subscribe();

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void shouldHandleExceptionalCompletion() {
        // Given
        String correlationId = "test-correlation-id";
        RuntimeException expectedException = new RuntimeException("Test error");
        Duration timeout = Duration.ofSeconds(1);

        // When
        Mono<String> result = asyncResponseManager.waitForResponse(
                correlationId, timeout, String.class
        );

        // Simulate error after delay
        Mono.delay(Duration.ofMillis(50))
                .doOnNext(ignore -> asyncResponseManager.completeRequestExceptionally(correlationId, expectedException))
                .subscribe();

        // Then
        StepVerifier.create(result)
                .expectErrorMessage("Test error")
                .verify();
    }
}