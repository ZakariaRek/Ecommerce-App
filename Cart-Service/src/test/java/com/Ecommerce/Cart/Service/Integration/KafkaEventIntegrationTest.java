// Cart-Service/src/test/java/com/Ecommerce/Cart/Service/Integration/KafkaEventIntegrationTest.java
package com.Ecommerce.Cart.Service.Integration;

import com.Ecommerce.Cart.Service.Config.KafkaProducerConfig;
import com.Ecommerce.Cart.Service.Events.CartItemEvents;
import com.Ecommerce.Cart.Service.Events.SavedForLaterEvents;
import com.Ecommerce.Cart.Service.Events.ShoppingCartEvents;
import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Services.Kafka.CartItemKafkaService;
import com.Ecommerce.Cart.Service.Services.Kafka.SavedForLaterKafkaService;
import com.Ecommerce.Cart.Service.Services.Kafka.ShoppingCartKafkaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Kafka Event Integration Tests")
class KafkaEventIntegrationTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withEmbeddedZookeeper();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.kafka.enabled", () -> "true");

        // Disable other services for isolated testing
        registry.add("spring.data.mongodb.host", () -> "localhost");
        registry.add("spring.data.mongodb.port", () -> "27017");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired
    private CartItemKafkaService cartItemKafkaService;

    @Autowired
    private SavedForLaterKafkaService savedForLaterKafkaService;

    @Autowired
    private ShoppingCartKafkaService shoppingCartKafkaService;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaConsumer<String, Object> testConsumer;
    private UUID userId;
    private UUID productId;
    private UUID cartId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        cartId = UUID.randomUUID();

        // Configure test consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);

        testConsumer = new KafkaConsumer<>(props);
    }

    @Test
    @DisplayName("Should publish cart item added event")
    void publishCartItemAddedEvent_ShouldSendEventToKafka() throws InterruptedException {
        // Arrange
        CartItem cartItem = createTestCartItem();
        CountDownLatch latch = new CountDownLatch(1);
        List<Object> receivedEvents = new ArrayList<>();

        // Subscribe to topic
        testConsumer.subscribe(Collections.singletonList(KafkaProducerConfig.TOPIC_CART_ITEM_ADDED));

        // Start consuming in background
        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Object> record : records) {
                    receivedEvents.add(record.value());
                    latch.countDown();
                }
            }
        });
        consumerThread.start();

        // Act
        cartItemKafkaService.publishCartItemAdded(cartItem, productId);

        // Assert
        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(receivedEvents).hasSize(1);

        // Verify event content
        Map<String, Object> eventMap = (Map<String, Object>) receivedEvents.get(0);
        assertThat(eventMap.get("eventType")).isEqualTo("CART_ITEM_ADDED");
        assertThat(eventMap.get("cartId")).isEqualTo(cartId.toString());
        assertThat(eventMap.get("productId")).isEqualTo(productId.toString());

        consumerThread.interrupt();
    }

    @Test
    @DisplayName("Should publish cart item removed event")
    void publishCartItemRemovedEvent_ShouldSendEventToKafka() throws InterruptedException {
        // Arrange
        CartItem cartItem = createTestCartItem();
        CountDownLatch latch = new CountDownLatch(1);
        List<Object> receivedEvents = new ArrayList<>();

        testConsumer.subscribe(Collections.singletonList(KafkaProducerConfig.TOPIC_CART_ITEM_REMOVED));

        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Object> record : records) {
                    receivedEvents.add(record.value());
                    latch.countDown();
                }
            }
        });
        consumerThread.start();

        // Act
        cartItemKafkaService.publishCartItemRemoved(cartItem, "user_removed");

        // Assert
        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(receivedEvents).hasSize(1);

        Map<String, Object> eventMap = (Map<String, Object>) receivedEvents.get(0);
        assertThat(eventMap.get("eventType")).isEqualTo("CART_ITEM_REMOVED");
        assertThat(eventMap.get("removalReason")).isEqualTo("user_removed");

        consumerThread.interrupt();
    }

    @Test
    @DisplayName("Should publish cart item quantity changed event")
    void publishCartItemQuantityChangedEvent_ShouldSendEventToKafka() throws InterruptedException {
        // Arrange
        CartItem cartItem = createTestCartItem();
        int oldQuantity = 1;
        CountDownLatch latch = new CountDownLatch(1);
        List<Object> receivedEvents = new ArrayList<>();

        testConsumer.subscribe(Collections.singletonList(KafkaProducerConfig.TOPIC_CART_ITEM_QUANTITY_CHANGED));

        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Object> record : records) {
                    receivedEvents.add(record.value());
                    latch.countDown();
                }
            }
        });
        consumerThread.start();

        // Act
        cartItemKafkaService.publishCartItemQuantityChanged(cartItem, oldQuantity);

        // Assert
        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(receivedEvents).hasSize(1);

        Map<String, Object> eventMap = (Map<String, Object>) receivedEvents.get(0);
        assertThat(eventMap.get("eventType")).isEqualTo("CART_ITEM_QUANTITY_CHANGED");
        assertThat(((Number) eventMap.get("oldQuantity")).intValue()).isEqualTo(oldQuantity);
        assertThat(((Number) eventMap.get("newQuantity")).intValue()).isEqualTo(cartItem.getQuantity());

        consumerThread.interrupt();
    }

    @Test
    @DisplayName("Should publish shopping cart created event")
    void publishCartCreatedEvent_ShouldSendEventToKafka() throws InterruptedException {
        // Arrange
        ShoppingCart cart = createTestShoppingCart();
        CountDownLatch latch = new CountDownLatch(1);
        List<Object> receivedEvents = new ArrayList<>();

        testConsumer.subscribe(Collections.singletonList(KafkaProducerConfig.TOPIC_CART_CREATED));

        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Object> record : records) {
                    receivedEvents.add(record.value());
                    latch.countDown();
                }
            }
        });
        consumerThread.start();

        // Act
        shoppingCartKafkaService.publishCartCreated(cart);

        // Assert
        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(receivedEvents).hasSize(1);

        Map<String, Object> eventMap = (Map<String, Object>) receivedEvents.get(0);
        assertThat(eventMap.get("eventType")).isEqualTo("CART_CREATED");
        assertThat(eventMap.get("cartId")).isEqualTo(cartId.toString());
        assertThat(eventMap.get("userId")).isEqualTo(userId.toString());

        consumerThread.interrupt();
    }

    @Test
    @DisplayName("Should publish saved for later event")
    void publishItemSavedForLaterEvent_ShouldSendEventToKafka() throws InterruptedException {
        // Arrange
        SavedForLater savedItem = createTestSavedItem();
        CountDownLatch latch = new CountDownLatch(1);
        List<Object> receivedEvents = new ArrayList<>();

        testConsumer.subscribe(Collections.singletonList(KafkaProducerConfig.TOPIC_ITEM_SAVED_FOR_LATER));

        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Object> record : records) {
                    receivedEvents.add(record.value());
                    latch.countDown();
                }
            }
        });
        consumerThread.start();

        // Act
        savedForLaterKafkaService.publishItemSavedForLater(savedItem);

        // Assert
        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertThat(eventReceived).isTrue();
        assertThat(receivedEvents).hasSize(1);

        Map<String, Object> eventMap = (Map<String, Object>) receivedEvents.get(0);
        assertThat(eventMap.get("eventType")).isEqualTo("ITEM_SAVED_FOR_LATER");
        assertThat(eventMap.get("userId")).isEqualTo(userId.toString());
        assertThat(eventMap.get("productId")).isEqualTo(productId.toString());

        consumerThread.interrupt();
    }

    @Test
    @DisplayName("Should publish multiple events in sequence")
    void publishMultipleEvents_ShouldSendAllEventsToKafka() throws InterruptedException {
        // Arrange
        CartItem cartItem = createTestCartItem();
        ShoppingCart cart = createTestShoppingCart();
        SavedForLater savedItem = createTestSavedItem();

        CountDownLatch latch = new CountDownLatch(3);
        List<Object> receivedEvents = new ArrayList<>();

        testConsumer.subscribe(Arrays.asList(
                KafkaProducerConfig.TOPIC_CART_ITEM_ADDED,
                KafkaProducerConfig.TOPIC_CART_CREATED,
                KafkaProducerConfig.TOPIC_ITEM_SAVED_FOR_LATER
        ));

        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Object> record : records) {
                    receivedEvents.add(record.value());
                    latch.countDown();
                }
            }
        });
        consumerThread.start();

        // Act
        cartItemKafkaService.publishCartItemAdded(cartItem, productId);
        shoppingCartKafkaService.publishCartCreated(cart);
        savedForLaterKafkaService.publishItemSavedForLater(savedItem);

        // Assert
        boolean allEventsReceived = latch.await(15, TimeUnit.SECONDS);
        assertThat(allEventsReceived).isTrue();
        assertThat(receivedEvents).hasSize(3);

        Set<String> eventTypes = new HashSet<>();
        for (Object event : receivedEvents) {
            Map<String, Object> eventMap = (Map<String, Object>) event;
            eventTypes.add((String) eventMap.get("eventType"));
        }

        assertThat(eventTypes).contains("CART_ITEM_ADDED", "CART_CREATED", "ITEM_SAVED_FOR_LATER");

        consumerThread.interrupt();
    }

    private CartItem createTestCartItem() {
        return CartItem.builder()
                .id(UUID.randomUUID())
                .cartId(cartId)
                .productId(productId)
                .quantity(2)
                .price(new BigDecimal("29.99"))
                .addedAt(LocalDateTime.now())
                .build();
    }

    private ShoppingCart createTestShoppingCart() {
        return ShoppingCart.builder()
                .id(cartId)
                .userId(userId)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private SavedForLater createTestSavedItem() {
        return SavedForLater.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(productId)
                .savedAt(LocalDateTime.now())
                .build();
    }
}