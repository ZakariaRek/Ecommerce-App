package com.Ecommerce.Gateway_Service.Integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class GatewayRoutesIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;

    @BeforeEach
    void setUp() {
        userServiceMock = new WireMockServer(8081);
        productServiceMock = new WireMockServer(8082);

        userServiceMock.start();
        productServiceMock.start();

        WireMock.configureFor("localhost", 8081);
    }

    @AfterEach
    void tearDown() {
        userServiceMock.stop();
        productServiceMock.stop();
    }

    @Test
    void shouldRouteToUserServiceForAuthentication() {
        // Given
        userServiceMock.stubFor(post(urlEqualTo("/api/users/auth/signin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"test-token\",\"username\":\"testuser\"}")));

        // When & Then
        webTestClient.post()
                .uri("/api/users/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"testuser\",\"password\":\"password\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("test-token");
    }

    @Test
    void shouldRouteToProductServiceForPublicAccess() {
        // Given
        productServiceMock.stubFor(get(urlEqualTo("/api/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":\"1\",\"name\":\"Test Product\"}]")));

        // When & Then
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldReturnUnauthorizedForProtectedEndpointWithoutToken() {
        // When & Then
        webTestClient.get()
                .uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowAccessToSwaggerUI() {
        // When & Then
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().isOk();
    }
}