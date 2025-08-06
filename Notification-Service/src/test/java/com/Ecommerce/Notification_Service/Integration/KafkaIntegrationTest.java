package com.Ecommerce.Notification_Service.Integration;

import com.Ecommerce.Notification_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Notification_Service.Listeners.AsynCom.EmailKafkaConsumer;
import com.Ecommerce.Notification_Service.Services.EmailService;
import com.Ecommerce.Notification_Service.Services.NotificationService;
import com.Ecommerce.Notification_Service.Services.UserEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext

@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserEmailService userEmailService;

    @Autowired
    private EmailKafkaConsumer emailKafkaConsumer;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        // Mock UserEmailService to return a successful future
        when(userEmailService.getUserInfo(any(UUID.class), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handlePaymentConfirmed_ShouldProcessKafkaMessage() throws Exception {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("userId", testUserId.toString());
        paymentEvent.put("orderId", "ORDER-123");
        paymentEvent.put("amount", "$99.99");
        paymentEvent.put("paymentMethod", "Credit Card");

        String messageJson = objectMapper.writeValueAsString(paymentEvent);

        // When
        kafkaTemplate.send("payment-confirmed", testUserId.toString(), messageJson);

        // Wait for message processing
        Thread.sleep(2000);

        // Then
        verify(notificationService, timeout(5000)).createNotification(
                eq(testUserId),
                any(),
                contains("Payment of $99.99 has been confirmed"),
                any()
        );
    }

    @Test
    void handleShippingUpdate_ShouldProcessKafkaMessage() throws Exception {
        // Given
        Map<String, Object> shippingEvent = new HashMap<>();
        shippingEvent.put("userId", testUserId.toString());
        shippingEvent.put("orderId", "ORDER-123");
        shippingEvent.put("status", "SHIPPED");
        shippingEvent.put("trackingNumber", "1Z999AA1234567890");

        String messageJson = objectMapper.writeValueAsString(shippingEvent);

        // When
        kafkaTemplate.send("shipping-update", testUserId.toString(), messageJson);

        // Wait for message processing
        Thread.sleep(2000);

        // Then
        verify(notificationService, timeout(5000)).createNotification(
                eq(testUserId),
                any(),
                contains("Shipping update for order #ORDER-123"),
                any()
        );
    }

    @Test
    void handleOrderStatusChanged_ShouldProcessKafkaMessage() throws Exception {
        // Given
        Map<String, Object> orderEvent = new HashMap<>();
        orderEvent.put("userId", testUserId.toString());
        orderEvent.put("orderId", "ORDER-123");
        orderEvent.put("oldStatus", "PENDING");
        orderEvent.put("newStatus", "CONFIRMED");

        String messageJson = objectMapper.writeValueAsString(orderEvent);

        // When
        kafkaTemplate.send("order-status-changed", testUserId.toString(), messageJson);

        // Wait for message processing
        Thread.sleep(2000);

        // Then
        verify(notificationService, timeout(5000)).createNotification(
                eq(testUserId),
                any(),
                contains("Your order #ORDER-123 status has been updated"),
                any()
        );
    }

    @Test
    void handleInventoryLowStock_ShouldProcessKafkaMessage() throws Exception {
        // Given
        Map<String, Object> inventoryEvent = new HashMap<>();
        inventoryEvent.put("productName", "Test Product");
        inventoryEvent.put("currentQuantity", 5);
        inventoryEvent.put("productId", "PROD-123");

        String messageJson = objectMapper.writeValueAsString(inventoryEvent);

        // When
        kafkaTemplate.send("inventory-low-stock", "inventory", messageJson);

        // Wait for message processing
        Thread.sleep(2000);

        // Then
        verify(notificationService, timeout(5000)).broadcastSystemNotification(
                eq("Low Stock Alert"),
                contains("Only 5 units left of Test Product"),
                any()
        );
    }

    @Test
    void handleProductCreated_ShouldProcessKafkaMessage() throws Exception {
        // Given
        Map<String, Object> productEvent = new HashMap<>();
        productEvent.put("name", "New Test Product");
        productEvent.put("productId", "PROD-456");

        String messageJson = objectMapper.writeValueAsString(productEvent);

        // When
        kafkaTemplate.send("product-created", "product", messageJson);

        // Wait for message processing
        Thread.sleep(2000);

        // Then
        // This would typically broadcast to connected users
        // We can verify through logs or mock the SSE service
    }

    @Test
    void kafkaProducer_ShouldSendMessage() throws JsonProcessingException {
        // Given
        Map<String, Object> testEvent = new HashMap<>();
        testEvent.put("testKey", "testValue");
        testEvent.put("timestamp", System.currentTimeMillis());

        // When
        CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> future =
                kafkaTemplate.send("payment-confirmed", "test-key", testEvent);

        // Then
        // Verify the message was sent (future completes without exception)
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send Kafka message", e);
        }
    }

    @Test
    void handleInvalidJsonMessage_ShouldNotThrowException() {
        // Given
        String invalidJson = "{ invalid json }";

        // When
        kafkaTemplate.send("payment-confirmed", testUserId.toString(), invalidJson);

        // Then
        // Should not throw exception - error should be logged
        // Verify no notifications are created
        verify(notificationService, after(3000).never()).createNotification(any(), any(), any(), any());
    }

    @Test
    void handleMessageWithMissingFields_ShouldHandleGracefully() throws Exception {
        // Given
        Map<String, Object> incompleteEvent = new HashMap<>();
        incompleteEvent.put("userId", testUserId.toString());
        // Missing orderId and other required fields

        String messageJson = objectMapper.writeValueAsString(incompleteEvent);

        // When
        kafkaTemplate.send("payment-confirmed", testUserId.toString(), messageJson);

        // Wait for message processing
        Thread.sleep(2000);

        // Then
        // Should handle gracefully and possibly create a generic notification
        // or log the error without crashing
    }

    @Test
    void multipleKafkaMessages_ShouldProcessAllMessages() throws Exception {
        // Given
        int messageCount = 5;

        for (int i = 0; i < messageCount; i++) {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", testUserId.toString());
            event.put("orderId", "ORDER-" + i);
            event.put("amount", "$" + (i * 10 + 50) + ".99");
            event.put("paymentMethod", "Credit Card");

            String messageJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("payment-confirmed", testUserId.toString(), messageJson);
        }

        // Wait for all messages to be processed
        Thread.sleep(5000);

        // Then
        verify(notificationService, timeout(10000).times(messageCount))
                .createNotification(eq(testUserId), any(), anyString(), any());
    }

    @Test
    void kafkaConsumerGroupHandling_ShouldProcessMessagesOnce() throws Exception {
        // Given
        Map<String, Object> event = new HashMap<>();
        event.put("userId", testUserId.toString());
        event.put("orderId", "ORDER-UNIQUE");
        event.put("amount", "$199.99");
        event.put("paymentMethod", "Credit Card");

        String messageJson = objectMapper.writeValueAsString(event);

        // When - Send same message multiple times to same topic
        kafkaTemplate.send("payment-confirmed", testUserId.toString(), messageJson);
        kafkaTemplate.send("payment-confirmed", testUserId.toString(), messageJson);
        kafkaTemplate.send("payment-confirmed", testUserId.toString(), messageJson);

        // Wait for message processing
        Thread.sleep(3000);

        // Then - Should process each message (they're not deduped by Kafka by default)
        verify(notificationService, timeout(5000).times(3))
                .createNotification(eq(testUserId), any(), contains("ORDER-UNIQUE"), any());
    }
}