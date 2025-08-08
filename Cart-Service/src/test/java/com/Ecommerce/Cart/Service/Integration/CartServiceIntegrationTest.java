// Cart-Service/src/test/java/com/Ecommerce/Cart/Service/Integration/CartServiceIntegrationTest.java
package com.Ecommerce.Cart.Service.Integration;

import com.Ecommerce.Cart.Service.Models.CartItem;
import com.Ecommerce.Cart.Service.Models.SavedForLater;
import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Payload.Request.AddItemRequest;
import com.Ecommerce.Cart.Service.Payload.Request.SaveForLaterRequest;
import com.Ecommerce.Cart.Service.Repositories.SavedForLaterRepository;
import com.Ecommerce.Cart.Service.Repositories.ShoppingCartRepository;
import com.Ecommerce.Cart.Service.Services.SavedForLaterService;
import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Cart Service Integration Tests")
class CartServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withExposedPorts(27017);

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", mongoDBContainer::getFirstMappedPort);
        registry.add("spring.data.mongodb.database", () -> "test-cart-service");

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);

        // Disable Kafka and Eureka for tests
        registry.add("spring.kafka.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ShoppingCartService cartService;

    @Autowired
    private SavedForLaterService savedForLaterService;

    @Autowired
    private ShoppingCartRepository cartRepository;

    @Autowired
    private SavedForLaterRepository savedForLaterRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Clean up data before each test
        mongoTemplate.getCollectionNames().forEach(mongoTemplate::dropCollection);
        cacheManager.getCacheNames().forEach(cacheName ->
                cacheManager.getCache(cacheName).clear());

        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should perform complete cart workflow")
    void completeCartWorkflow_ShouldWork() throws Exception {
        // 1. Create cart and add item
        AddItemRequest addItemRequest = AddItemRequest.builder()
                .productId(productId)
                .quantity(2)
                .price(new BigDecimal("29.99"))
                .build();

        mockMvc.perform(post("/{userId}/items", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Verify cart in database
        ShoppingCart savedCart = cartRepository.findByUserId(userId).orElse(null);
        assertThat(savedCart).isNotNull();
        assertThat(savedCart.getItems()).hasSize(1);
        assertThat(savedCart.getItems().get(0).getProductId()).isEqualTo(productId);

        // 3. Get cart and verify caching
        mockMvc.perform(get("/{userId}", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        // Verify cache is populated
        assertThat(cacheManager.getCache("shoppingCarts").get(userId.toString())).isNotNull();

        // 4. Calculate total
        mockMvc.perform(get("/{userId}/total", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(59.98));
    }

    @Test
    @DisplayName("Should perform complete saved for later workflow")
    void completeSavedForLaterWorkflow_ShouldWork() throws Exception {
        // 1. Save item for later
        SaveForLaterRequest saveRequest = SaveForLaterRequest.builder()
                .productId(productId)
                .build();

        mockMvc.perform(post("/{userId}/saved", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Verify saved item in database
        List<SavedForLater> savedItems = savedForLaterRepository.findByUserId(userId);
        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getProductId()).isEqualTo(productId);

        // 3. Get saved items
        mockMvc.perform(get("/{userId}/saved", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].productId").value(productId.toString()));

        // 4. Check saved items count
        mockMvc.perform(get("/{userId}/saved/count", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        // 5. Check if product is saved
        mockMvc.perform(get("/{userId}/saved/{productId}/exists", userId.toString(), productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("Should handle service layer integration")
    void serviceLayerIntegration_ShouldWork() {
        // Test service layer directly
        ShoppingCart cart = cartService.getOrCreateCart(userId);
        assertThat(cart).isNotNull();
        assertThat(cart.getUserId()).isEqualTo(userId);

        // Add item using service
        cartService.addItemToCart(userId, productId, 3, new BigDecimal("19.99"));

        // Verify item was added
        ShoppingCart updatedCart = cartService.getOrCreateCart(userId);
        assertThat(updatedCart.getItems()).hasSize(1);
        assertThat(updatedCart.getItems().get(0).getQuantity()).isEqualTo(3);

        // Test saved for later service
        SavedForLater savedItem = savedForLaterService.saveForLater(userId, UUID.randomUUID());
        assertThat(savedItem).isNotNull();

        List<SavedForLater> savedItems = savedForLaterService.getSavedItems(userId);
        assertThat(savedItems).hasSize(1);
    }

    @Test
    @DisplayName("Should handle cache integration")
    void cacheIntegration_ShouldWork() {
        // Get cart first time (should cache)
        ShoppingCart cart1 = cartService.getOrCreateCart(userId);
        assertThat(cart1).isNotNull();

        // Verify cache is populated
        assertThat(cacheManager.getCache("shoppingCarts").get(userId.toString())).isNotNull();

        // Get cart second time (should hit cache)
        ShoppingCart cart2 = cartService.getOrCreateCart(userId);
        assertThat(cart2).isNotNull();
        assertThat(cart1.getId()).isEqualTo(cart2.getId());

        // Add item (should evict cache)
        cartService.addItemToCart(userId, productId, 1, new BigDecimal("10.00"));

        // Get cart again (should reload from database)
        ShoppingCart cart3 = cartService.getOrCreateCart(userId);
        assertThat(cart3.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle error scenarios")
    void errorScenarios_ShouldWork() throws Exception {
        // Test invalid UUID
        mockMvc.perform(get("/invalid-uuid"))
                .andExpect(status().isBadRequest());

        // Test invalid request body
        mockMvc.perform(post("/{userId}/items", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Test saving duplicate item
        savedForLaterService.saveForLater(userId, productId);

        SaveForLaterRequest duplicateRequest = SaveForLaterRequest.builder()
                .productId(productId)
                .build();

        mockMvc.perform(post("/{userId}/saved", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should handle concurrent operations")
    void concurrentOperations_ShouldWork() throws InterruptedException {
        // Simulate concurrent operations
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                cartService.addItemToCart(userId, UUID.randomUUID(), 1, new BigDecimal("10.00"));
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                savedForLaterService.saveForLater(userId, UUID.randomUUID());
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Verify results
        ShoppingCart cart = cartService.getOrCreateCart(userId);
        List<SavedForLater> savedItems = savedForLaterService.getSavedItems(userId);

        assertThat(cart.getItems()).hasSize(5);
        assertThat(savedItems).hasSize(5);
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/carts";
    }
}