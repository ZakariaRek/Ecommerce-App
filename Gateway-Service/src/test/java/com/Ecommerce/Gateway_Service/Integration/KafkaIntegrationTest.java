package com.Ecommerce.Gateway_Service.Integration;

import com.Ecommerce.Gateway_Service.Consumer.KafkaCartResponseConsumer;
import com.Ecommerce.Gateway_Service.DTOs.EnrichedShoppingCartResponse;
import com.Ecommerce.Gateway_Service.Kafka.AsyncResponseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"cart.response", "cart.error"})
@ActiveProfiles("test")
class KafkaIntegrationTest {

    @Autowired
    private KafkaCartResponseConsumer cartResponseConsumer;

    @Autowired
    private AsyncResponseManager asyncResponseManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldProcessSuccessfulCartResponse() {
        // Given
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> responsePayload = createSuccessfulCartResponse(correlationId);

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "cart.response", 0, 0, correlationId, responsePayload
        );

        AsyncResponseManager mockAsyncResponseManager = mock(AsyncResponseManager.class);

        KafkaCartResponseConsumer consumer = new KafkaCartResponseConsumer(
                mockAsyncResponseManager, objectMapper
        );

        // When
        consumer.handleCartResponse(record);

        // Then
        verify(mockAsyncResponseManager).completeRequest(
                eq(correlationId),
                any(EnrichedShoppingCartResponse.EnrichedCartResponseDTO.class)
        );
    }

    @Test
    void shouldProcessErrorCartResponse() {
        // Given
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> errorPayload = createErrorCartResponse(correlationId);

        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "cart.response", 0, 0, correlationId, errorPayload
        );

        AsyncResponseManager mockAsyncResponseManager = mock(AsyncResponseManager.class);

        KafkaCartResponseConsumer consumer = new KafkaCartResponseConsumer(
                mockAsyncResponseManager, objectMapper
        );

        // When
        consumer.handleCartResponse(record);

        // Then
        verify(mockAsyncResponseManager).completeRequestExceptionally(
                eq(correlationId),
                any(RuntimeException.class)
        );
    }

    private Map<String, Object> createSuccessfulCartResponse(String correlationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("success", true);
        response.put("message", "Cart retrieved successfully");

        Map<String, Object> cartData = new HashMap<>();
        cartData.put("id", UUID.randomUUID().toString());
        cartData.put("userId", UUID.randomUUID().toString());
        cartData.put("items", List.of());
        cartData.put("total", 0.0);
        cartData.put("itemCount", 0);
        cartData.put("totalQuantity", 0);

        response.put("data", cartData);
        return response;
    }

    private Map<String, Object> createErrorCartResponse(String correlationId) {
        Map<String, Object> response = new HashMap<>();
        response.put("correlationId", correlationId);
        response.put("success", false);
        response.put("message", "Cart not found");
        return response;
    }
}