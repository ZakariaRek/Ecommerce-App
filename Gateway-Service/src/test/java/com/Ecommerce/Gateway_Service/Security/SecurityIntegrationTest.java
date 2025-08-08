package com.Ecommerce.Gateway_Service.Security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRejectRequestWithInvalidJWT() {
        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectRequestWithExpiredJWT() {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTYwOTQ1OTIwMH0.invalid";

        webTestClient.get()
                .uri("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectRequestWithoutRequiredRole() {
        String userToken = createTokenWithRole("ROLE_USER");

        webTestClient.get()
                .uri("/api/users/admin/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldAllowPublicEndpointsWithoutAuthentication() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldPreventJWTTokenInUrl() {
        webTestClient.get()
                .uri("/api/users/profile?token=some-jwt-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String createTokenWithRole(String role) {
        // Implementation to create test JWT tokens
        return "valid-test-token-with-" + role;
    }
}