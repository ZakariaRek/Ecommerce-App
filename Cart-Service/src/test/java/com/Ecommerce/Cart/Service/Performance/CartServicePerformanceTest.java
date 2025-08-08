// Cart-Service/src/test/java/com/Ecommerce/Cart/Service/Performance/CartServicePerformanceTest.java
package com.Ecommerce.Cart.Service.Performance;

import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import com.Ecommerce.Cart.Service.Services.SavedForLaterService;
import com.Ecommerce.Cart.Service.Services.ShoppingCartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Cart Service Performance Tests")
class CartServicePerformanceTest {

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
        registry.add("spring.data.mongodb.database", () -> "test-cart-performance");

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);

        // Disable external services
        registry.add("spring.kafka.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired
    private ShoppingCartService cartService;

    @Autowired
    private SavedForLaterService savedForLaterService;

    @Autowired
    private CacheManager cacheManager;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(10);
        // Clear cache before each test
        cacheManager.getCacheNames().forEach(cacheName ->
                cacheManager.getCache(cacheName).clear());
    }

    @Test
    @DisplayName("Should handle concurrent cart operations")
    void concurrentCartOperations_ShouldPerformWell() throws InterruptedException, ExecutionException {
        // Arrange
        int numberOfUsers = 100;
        int operationsPerUser = 10;
        List<UUID> userIds = IntStream.range(0, numberOfUsers)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Act
        Instant start = Instant.now();

        List<CompletableFuture<Void>> futures = userIds.stream()
                .map(userId -> CompletableFuture.runAsync(() -> {
                    try {
                        for (int i = 0; i < operationsPerUser; i++) {
                            // Perform various cart operations
                            cartService.addItemToCart(userId, UUID.randomUUID(), 1, new BigDecimal("10.00"));
                            cartService.calculateCartTotal(userId);
                            cartService.getOrCreateCart(userId);
                        }
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        e.printStackTrace();
                    }
                }, executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        Duration duration = Duration.between(start, Instant.now());

        // Assert
        assertThat(successCount.get()).isEqualTo(numberOfUsers);
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(duration.toMillis()).isLessThan(30000); // Should complete within 30 seconds

        System.out.printf("Performance Test Results:%n");
        System.out.printf("Users: %d, Operations per user: %d%n", numberOfUsers, operationsPerUser);
        System.out.printf("Total operations: %d%n", numberOfUsers * operationsPerUser * 3);
        System.out.printf("Duration: %d ms%n", duration.toMillis());
        System.out.printf("Operations per second: %.2f%n",
                (double) (numberOfUsers * operationsPerUser * 3) / duration.toSeconds());
    }

    @Test
    @DisplayName("Should demonstrate cache performance benefits")
    void cachePerformance_ShouldShowImprovement() {
        // Arrange
        UUID userId = UUID.randomUUID();
        int iterations = 1000;

        // Add some items to cart first
        for (int i = 0; i < 5; i++) {
            cartService.addItemToCart(userId, UUID.randomUUID(), 1, new BigDecimal("10.00"));
        }

        // Act & Assert - First run (cold cache)
        Instant start1 = Instant.now();
        for (int i = 0; i < iterations; i++) {
            cartService.getOrCreateCart(userId);
            // Clear cache to force database access
            cacheManager.getCache("shoppingCarts").clear();
        }
        Duration coldCacheDuration = Duration.between(start1, Instant.now());

        // Second run (warm cache)
        Instant start2 = Instant.now();
        for (int i = 0; i < iterations; i++) {
            cartService.getOrCreateCart(userId);
            // Don't clear cache this time
        }
        Duration warmCacheDuration = Duration.between(start2, Instant.now());

        System.out.printf("Cache Performance Test Results:%n");
        System.out.printf("Cold cache duration: %d ms%n", coldCacheDuration.toMillis());
        System.out.printf("Warm cache duration: %d ms%n", warmCacheDuration.toMillis());
        System.out.printf("Performance improvement: %.2fx%n",
                (double) coldCacheDuration.toMillis() / warmCacheDuration.toMillis());

        // Warm cache should be significantly faster
        assertThat(warmCacheDuration.toMillis()).isLessThan(coldCacheDuration.toMillis() / 2);
    }

    @Test
    @DisplayName("Should handle high load cart additions")
    void highLoadCartAdditions_ShouldPerformWell() throws InterruptedException, ExecutionException {
        // Arrange
        UUID userId = UUID.randomUUID();
        int numberOfItems = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfItems);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act
        Instant start = Instant.now();

        List<CompletableFuture<Void>> futures = IntStream.range(0, numberOfItems)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                    try {
                        cartService.addItemToCart(userId, UUID.randomUUID(), 1,
                                new BigDecimal(String.valueOf(10 + i % 100)));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }, executorService))
                .toList();

        latch.await(60, TimeUnit.SECONDS);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        Duration duration = Duration.between(start, Instant.now());

        // Assert
        ShoppingCart finalCart = cartService.getOrCreateCart(userId);

        System.out.printf("High Load Test Results:%n");
        System.out.printf("Items added: %d%n", numberOfItems);
        System.out.printf("Successful operations: %d%n", successCount.get());
        System.out.printf("Final cart size: %d%n", finalCart.getItems().size());
        System.out.printf("Duration: %d ms%n", duration.toMillis());
        System.out.printf("Items per second: %.2f%n",
                (double) numberOfItems / duration.toSeconds());

        assertThat(successCount.get()).isGreaterThan((int) (numberOfItems * 0.95)); // 95% success rate
        assertThat(duration.toMillis()).isLessThan(60000); // Should complete within 60 seconds
    }

    @Test
    @DisplayName("Should handle bulk saved items operations efficiently")
    void bulkSavedItemsOperations_ShouldPerformWell() throws InterruptedException, ExecutionException {
        // Arrange
        int numberOfUsers = 50;
        int itemsPerUser = 20;
        AtomicInteger totalOperations = new AtomicInteger(0);

        // Act
        Instant start = Instant.now();

        List<CompletableFuture<Void>> futures = IntStream.range(0, numberOfUsers)
                .mapToObj(i -> {
                    UUID userId = UUID.randomUUID();
                    return CompletableFuture.runAsync(() -> {
                        try {
                            // Add items to saved for later
                            for (int j = 0; j < itemsPerUser; j++) {
                                savedForLaterService.saveForLater(userId, UUID.randomUUID());
                                totalOperations.incrementAndGet();
                            }

                            // Get saved items
                            savedForLaterService.getSavedItems(userId);
                            totalOperations.incrementAndGet();

                            // Get count
                            savedForLaterService.getSavedItemCount(userId);
                            totalOperations.incrementAndGet();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, executorService);
                })
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        Duration duration = Duration.between(start, Instant.now());

        // Assert
        System.out.printf("Bulk Saved Items Test Results:%n");
        System.out.printf("Users: %d, Items per user: %d%n", numberOfUsers, itemsPerUser);
        System.out.printf("Total operations: %d%n", totalOperations.get());
        System.out.printf("Duration: %d ms%n", duration.toMillis());
        System.out.printf("Operations per second: %.2f%n",
                (double) totalOperations.get() / duration.toSeconds());

        assertThat(duration.toMillis()).isLessThan(30000); // Should complete within 30 seconds
    }

    @Test
    @DisplayName("Should handle memory usage efficiently")
    void memoryUsage_ShouldBeEfficient() {
        // Arrange
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        List<UUID> userIds = new ArrayList<>();
        int numberOfUsers = 1000;

        // Act
        for (int i = 0; i < numberOfUsers; i++) {
            UUID userId = UUID.randomUUID();
            userIds.add(userId);

            // Create cart and add items
            for (int j = 0; j < 10; j++) {
                cartService.addItemToCart(userId, UUID.randomUUID(), 1, new BigDecimal("10.00"));
            }

            // Add saved items
            for (int j = 0; j < 5; j++) {
                savedForLaterService.saveForLater(userId, UUID.randomUUID());
            }
        }

        // Force garbage collection
        System.gc();

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        long memoryPerUser = memoryUsed / numberOfUsers;

        // Assert
        System.out.printf("Memory Usage Test Results:%n");
        System.out.printf("Users created: %d%n", numberOfUsers);
        System.out.printf("Memory used: %d bytes (%.2f MB)%n", memoryUsed, memoryUsed / 1024.0 / 1024.0);
        System.out.printf("Memory per user: %d bytes%n", memoryPerUser);

        // Each user should use less than 10KB of memory
        assertThat(memoryPerUser).isLessThan(10240);
    }

    @Test
    @DisplayName("Should maintain performance under sustained load")
    void sustainedLoad_ShouldMaintainPerformance() throws InterruptedException {
        // Arrange
        UUID userId = UUID.randomUUID();
        AtomicInteger operationCount = new AtomicInteger(0);
         boolean keepRunning = true;

        // Act - Run for 30 seconds
        boolean finalKeepRunning = keepRunning;
        CompletableFuture<Void> loadTest = CompletableFuture.runAsync(() -> {
            while (finalKeepRunning) {
                try {
                    cartService.addItemToCart(userId, UUID.randomUUID(), 1, new BigDecimal("10.00"));
                    cartService.getOrCreateCart(userId);
                    savedForLaterService.saveForLater(userId, UUID.randomUUID());
                    operationCount.addAndGet(3);

                    Thread.sleep(10); // Small delay to simulate realistic load
                } catch (Exception e) {
                    // Ignore errors for this test
                }
            }
        });

        Thread.sleep(30000); // Run for 30 seconds
        keepRunning = false;

        loadTest.join();

        // Assert
        int totalOperations = operationCount.get();
        double operationsPerSecond = totalOperations / 30.0;

        System.out.printf("Sustained Load Test Results:%n");
        System.out.printf("Duration: 30 seconds%n");
        System.out.printf("Total operations: %d%n", totalOperations);
        System.out.printf("Operations per second: %.2f%n", operationsPerSecond);

        // Should maintain at least 10 operations per second
        assertThat(operationsPerSecond).isGreaterThan(10.0);
    }
}