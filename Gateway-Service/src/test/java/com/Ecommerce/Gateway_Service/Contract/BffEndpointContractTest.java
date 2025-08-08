package com.Ecommerce.Gateway_Service.Contract;

import com.Ecommerce.Gateway_Service.Service.AsyncCartBffService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class BffEndpointContractTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void enrichedCartEndpointShouldReturnCorrectStructure() {
        String validUserId = "123e4567-e89b-12d3-a456-426614174000";
        String validToken = createValidJWTToken();

        webTestClient.get()
                .uri("/api/cart/{userId}/enriched", validUserId)
                .header("Authorization", "Bearer " + validToken)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.userId").isEqualTo(validUserId)
                .jsonPath("$.items").isArray()
                .jsonPath("$.total").isNumber()
                .jsonPath("$.itemCount").isNumber()
                .jsonPath("$.totalQuantity").isNumber()
                .jsonPath("$.createdAt").exists()
                .jsonPath("$.updatedAt").exists();
    }

    @Test
    void enrichedOrderEndpointShouldReturnCorrectStructure() {
        String validOrderId = "123e4567-e89b-12d3-a456-426614174001";
        String validToken = createValidJWTToken();

        webTestClient.get()
                .uri("/api/order/{orderId}/enriched", validOrderId)
                .header("Authorization", "Bearer " + validToken)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.userId").exists()
                .jsonPath("$.status").exists()
                .jsonPath("$.items").isArray()
                .jsonPath("$.totalAmount").isNumber()
                .jsonPath("$.createdAt").exists();
    }

    @Test
    void batchOrderEndpointShouldHandleValidRequest() {
        String validToken = createValidJWTToken();

        String requestBody = """
            {
                "orderIds": ["order1", "order2"],
                "includeProducts": true
            }
            """;

        webTestClient.post()
                .uri("/api/order/batch")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.orders").isArray()
                .jsonPath("$.failures").exists()
                .jsonPath("$.totalRequested").isNumber()
                .jsonPath("$.successful").isNumber()
                .jsonPath("$.failed").isNumber()
                .jsonPath("$.includeProducts").isBoolean()
                .jsonPath("$.processingTimeMs").isNumber();
    }

    private String createValidJWTToken() {
        // Implementation to create valid test JWT
        return "valid-test-jwt-token";
    }
}