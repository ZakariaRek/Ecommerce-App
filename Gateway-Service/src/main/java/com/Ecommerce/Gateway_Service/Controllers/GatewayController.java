package com.Ecommerce.Gateway_Service.Controllers;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/gateway")
@Tag(name = "Gateway Management", description = "Gateway management and monitoring endpoints")
public class GatewayController {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Operation(
            summary = "Get all circuit breakers status",
            description = "Returns the status of all configured circuit breakers in the gateway"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved circuit breakers status",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CircuitBreakerStatusResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getAllCircuitBreakers() {
        Map<String, Object> response = new HashMap<>();

        List<CircuitBreakerInfo> circuitBreakers = StreamSupport.stream(
                        circuitBreakerRegistry.getAllCircuitBreakers().spliterator(), false)
                .map(this::mapToCircuitBreakerInfo)
                .collect(Collectors.toList());

        response.put("timestamp", LocalDateTime.now());
        response.put("totalCircuitBreakers", circuitBreakers.size());
        response.put("circuitBreakers", circuitBreakers);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get specific circuit breaker status",
            description = "Returns the status of a specific circuit breaker by name"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved circuit breaker status",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CircuitBreakerInfo.class))),
            @ApiResponse(responseCode = "404", description = "Circuit breaker not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/circuit-breakers/{name}")
    public ResponseEntity<CircuitBreakerInfo> getCircuitBreaker(
            @Parameter(description = "Name of the circuit breaker", required = true)
            @PathVariable String name) {

        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            CircuitBreakerInfo info = mapToCircuitBreakerInfo(circuitBreaker);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Reset circuit breaker",
            description = "Resets a specific circuit breaker to CLOSED state",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Circuit breaker reset successfully"),
            @ApiResponse(responseCode = "404", description = "Circuit breaker not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<Map<String, String>> resetCircuitBreaker(
            @Parameter(description = "Name of the circuit breaker to reset", required = true)
            @PathVariable String name) {

        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            circuitBreaker.reset();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Circuit breaker '" + name + "' has been reset");
            response.put("status", circuitBreaker.getState().toString());
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Get gateway health status",
            description = "Returns the overall health status of the gateway and its services"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved gateway health",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GatewayHealthResponse.class)))
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getGatewayHealth() {
        Map<String, Object> health = new HashMap<>();

        List<CircuitBreaker> allCircuitBreakers = StreamSupport.stream(
                        circuitBreakerRegistry.getAllCircuitBreakers().spliterator(), false)
                .collect(Collectors.toList());

        long openCircuitBreakers = allCircuitBreakers.stream()
                .filter(cb -> cb.getState() == CircuitBreaker.State.OPEN)
                .count();

        long halfOpenCircuitBreakers = allCircuitBreakers.stream()
                .filter(cb -> cb.getState() == CircuitBreaker.State.HALF_OPEN)
                .count();

        String overallStatus = openCircuitBreakers > 0 ? "DEGRADED" :
                halfOpenCircuitBreakers > 0 ? "RECOVERING" : "HEALTHY";

        health.put("status", overallStatus);
        health.put("timestamp", LocalDateTime.now());
        health.put("circuitBreakers", Map.of(
                "total", allCircuitBreakers.size(),
                "open", openCircuitBreakers,
                "halfOpen", halfOpenCircuitBreakers,
                "closed", allCircuitBreakers.size() - openCircuitBreakers - halfOpenCircuitBreakers
        ));

        return ResponseEntity.ok(health);
    }

    @Operation(
            summary = "Get available services",
            description = "Returns list of all services routed through the gateway"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved services list")
    })
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getServices() {
        Map<String, Object> services = new HashMap<>();

        List<ServiceInfo> serviceList = List.of(
                new ServiceInfo("user-service", "User Management Service", "/api/users/**", "lb://user-service"),
                new ServiceInfo("product-service", "Product Catalog Service", "/api/products/**", "lb://product-service"),
                new ServiceInfo("order-service", "Order Management Service", "/api/orders/**", "lb://ORDER-SERVICE"),
                new ServiceInfo("payment-service", "Payment Processing Service", "/api/payments/**", "lb://PAYMENT-SERVICE"),
                new ServiceInfo("cart-service", "Shopping Cart Service", "/api/cart/**", "lb://CART-SERVICE"),

                new ServiceInfo("loyalty-service", "Loyalty Program Service", "/api/loyalty/**", "lb://LOYALTY-SERVICE"),
                new ServiceInfo("notification-service", "Notification Service", "/api/notifications/**", "lb://NOTIFICATION-SERVICE"),
                new ServiceInfo("shipping-service", "Shipping Service", "/api/shipping/**", "lb://SHIPPING-SERVICE")
        );

        services.put("timestamp", LocalDateTime.now());
        services.put("totalServices", serviceList.size());
        services.put("services", serviceList);

        return ResponseEntity.ok(services);
    }

    private CircuitBreakerInfo mapToCircuitBreakerInfo(CircuitBreaker circuitBreaker) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        return new CircuitBreakerInfo(
                circuitBreaker.getName(),
                circuitBreaker.getState().toString(),
                metrics.getNumberOfSuccessfulCalls(),
                metrics.getNumberOfFailedCalls(),
                metrics.getFailureRate(),
                metrics.getNumberOfBufferedCalls(),
                circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
                circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize()
        );
    }

    // DTOs for API responses
    public static class CircuitBreakerInfo {
        private String name;
        private String state;
        private int successfulCalls;
        private int failedCalls;
        private float failureRate;
        private int bufferedCalls;
        private float failureRateThreshold;
        private int slidingWindowSize;

        public CircuitBreakerInfo(String name, String state, int successfulCalls, int failedCalls,
                                  float failureRate, int bufferedCalls, float failureRateThreshold, int slidingWindowSize) {
            this.name = name;
            this.state = state;
            this.successfulCalls = successfulCalls;
            this.failedCalls = failedCalls;
            this.failureRate = failureRate;
            this.bufferedCalls = bufferedCalls;
            this.failureRateThreshold = failureRateThreshold;
            this.slidingWindowSize = slidingWindowSize;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public int getSuccessfulCalls() { return successfulCalls; }
        public void setSuccessfulCalls(int successfulCalls) { this.successfulCalls = successfulCalls; }

        public int getFailedCalls() { return failedCalls; }
        public void setFailedCalls(int failedCalls) { this.failedCalls = failedCalls; }

        public float getFailureRate() { return failureRate; }
        public void setFailureRate(float failureRate) { this.failureRate = failureRate; }

        public int getBufferedCalls() { return bufferedCalls; }
        public void setBufferedCalls(int bufferedCalls) { this.bufferedCalls = bufferedCalls; }

        public float getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(float failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }

        public int getSlidingWindowSize() { return slidingWindowSize; }
        public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }
    }

    public static class ServiceInfo {
        private String name;
        private String description;
        private String path;
        private String uri;

        public ServiceInfo(String name, String description, String path, String uri) {
            this.name = name;
            this.description = description;
            this.path = path;
            this.uri = uri;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
    }

    // Schema classes for Swagger documentation
    public static class CircuitBreakerStatusResponse {
        public LocalDateTime timestamp;
        public int totalCircuitBreakers;
        public List<CircuitBreakerInfo> circuitBreakers;
    }

    public static class GatewayHealthResponse {
        public String status;
        public LocalDateTime timestamp;
        public Map<String, Object> circuitBreakers;
    }
}