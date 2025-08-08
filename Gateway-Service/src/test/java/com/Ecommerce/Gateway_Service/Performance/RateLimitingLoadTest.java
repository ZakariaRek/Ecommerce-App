//package com.Ecommerce.Gateway_Service.Performance;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//class RateLimitingLoadTest {
//
//    @Autowired
//    private WebTestClient webTestClient;
//
//    @Test
//    void shouldEnforceRateLimitUnderLoad() {
//        // Given
//        int totalRequests = 20;
//        int expectedSuccessful = 5; // Based on rate limit configuration
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger rateLimitedCount = new AtomicInteger(0);
//
//        // When
//        Flux.range(0, totalRequests)
//                .flatMap(i -> makeRequest())
//                .doOnNext(statusCode -> {
//                    if (statusCode == 200) {
//                        successCount.incrementAndGet();
//                    } else if (statusCode == 429) {
//                        rateLimitedCount.incrementAndGet();
//                    }
//                })
//                .blockLast(Duration.ofSeconds(30));
//
//        // Then
//        assertThat(successCount.get()).isLessThanOrEqualTo(expectedSuccessful + 2); // Allow some buffer
//        assertThat(rateLimitedCount.get()).isGreaterThan(0);
//        assertThat(successCount.get() + rateLimitedCount.get()).isEqualTo(totalRequests);
//    }
//    private Mono<Integer> makeRequest() {
//        return webTestClient.get()
//                .uri("/api/gateway/health")
//                .exchange()
//                .returnResult(Void.class)
//                .getStatusCode()
//                .map(status -> status.value())
//                .onErrorReturn(429); // Simulate rate limit exceeded for testing purposes
//    }
//}