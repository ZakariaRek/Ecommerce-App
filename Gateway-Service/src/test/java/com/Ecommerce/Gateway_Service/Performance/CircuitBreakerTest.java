//package com.Ecommerce.Gateway_Service.Performance;
//
//import com.github.tomakehurst.wiremock.WireMockServer;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.HttpStatus;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.reactive.server.WebTestClient;
//import reactor.core.publisher.Flux;
//
//import java.time.Duration;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.*;
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")
//class CircuitBreakerTest {
//
//    @Autowired
//    private WebTestClient webTestClient;
//
//    private WireMockServer mockServer;
//
//    @BeforeEach
//    void setUp() {
//        mockServer = new WireMockServer(8081);
//        mockServer.start();
//    }
//
//    @AfterEach
//    void tearDown() {
//        mockServer.stop();
//    }
//
//    @Test
//    void shouldOpenCircuitBreakerAfterFailures() {
//        // Given - Configure mock to return errors
//        mockServer.stubFor(get(urlMatching("/api/users/.*"))
//                .willReturn(aResponse()
//                        .withStatus(500)
//                        .withFixedDelay(1000)));
//
//        AtomicInteger errorCount = new AtomicInteger(0);
//        AtomicInteger circuitOpenCount = new AtomicInteger(0);
//
//        // When - Make multiple requests to trigger circuit breaker
//        Flux.range(0, 15)
//                .delayElements(Duration.ofMillis(100))
//                .flatMap(i ->
//                        webTestClient.get()
//                                .uri("/api/users/profile")
//                                .header("Authorization", "Bearer valid-token")
//                                .exchange()
//                                .map(response -> response.statusCode().value())
//                )
//                .doOnNext(status -> {
//                    if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
//                        errorCount.incrementAndGet();
//                    } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
//                        circuitOpenCount.incrementAndGet();
//                    }
//                })
//                .blockLast(Duration.ofSeconds(30));
//
//        // Then - Circuit breaker should have opened
//        assertThat(errorCount.get()).isGreaterThan(0);
//        assertThat(circuitOpenCount.get()).isGreaterThan(0);
//    }
//}