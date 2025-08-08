package com.Ecommerce.Gateway_Service.Management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayManagementApiTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnCircuitBreakerStatus() {
        webTestClient.get()
                .uri("/api/gateway/circuit-breakers")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.totalCircuitBreakers").isNumber()
                .jsonPath("$.circuitBreakers").isArray();
    }

    @Test
    void shouldReturnGatewayHealth() {
        webTestClient.get()
                .uri("/api/gateway/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.circuitBreakers").exists();
    }

    @Test
    void shouldReturnServicesList() {
        webTestClient.get()
                .uri("/api/gateway/services")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.totalServices").isNumber()
                .jsonPath("$.services").isArray()
                .jsonPath("$.services[0].name").exists()
                .jsonPath("$.services[0].description").exists()
                .jsonPath("$.services[0].path").exists()
                .jsonPath("$.services[0].uri").exists();
    }

    @Test
    void shouldReturnRateLimitingConfig() {
        webTestClient.get()
                .uri("/api/gateway/rate-limiting/config")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.totalEndpoints").isNumber()
                .jsonPath("$.endpoints").isArray()
                .jsonPath("$.endpoints[0].name").exists()
                .jsonPath("$.endpoints[0].limit").exists()
                .jsonPath("$.endpoints[0].windowSeconds").exists();
    }
}