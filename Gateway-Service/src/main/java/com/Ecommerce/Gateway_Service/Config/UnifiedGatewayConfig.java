package com.Ecommerce.Gateway_Service.Config;

import com.Ecommerce.Gateway_Service.Security.CustomRateLimitFilterFactory;
import com.Ecommerce.Gateway_Service.Security.JwtAuthenticationFilterFactory;
import com.Ecommerce.Gateway_Service.Security.TokenBucketRateLimitFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class UnifiedGatewayConfig {

    private final JwtAuthenticationFilterFactory jwtAuthenticationFilterFactory;
    private final CustomRateLimitFilterFactory customRateLimitFilterFactory;
    private final TokenBucketRateLimitFilterFactory tokenBucketRateLimitFilterFactory;

    public UnifiedGatewayConfig(
            JwtAuthenticationFilterFactory jwtAuthenticationFilterFactory,
            CustomRateLimitFilterFactory customRateLimitFilterFactory,
            TokenBucketRateLimitFilterFactory tokenBucketRateLimitFilterFactory) {
        this.jwtAuthenticationFilterFactory = jwtAuthenticationFilterFactory;
        this.customRateLimitFilterFactory = customRateLimitFilterFactory;
        this.tokenBucketRateLimitFilterFactory = tokenBucketRateLimitFilterFactory;
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Gateway's own Swagger UI
                .route("gateway-swagger-ui", r -> r
                        .path("/swagger-ui/**", "/swagger-ui.html")
                        .uri("forward:/"))

                // Gateway's own API docs
                .route("gateway-api-docs", r -> r
                        .path("/v3/api-docs/**")
                        .uri("forward:/"))

                // Swagger resources
                .route("swagger-resources", r -> r
                        .path("/swagger-resources/**")
                        .uri("forward:/"))

                // Root redirect to Swagger UI
                .route("docs-redirect", r -> r
                        .path("/docs")
                        .filters(f -> f.redirect(302, "/swagger-ui.html"))
                        .uri("forward:/"))

                // Authentication endpoints - More restrictive rate limiting
                .route("user-service-auth", r -> r
                        .path("/api/users/auth/**")
                        .filters(f -> f
                                .filter(customRateLimitFilterFactory.apply(createConfig(5, 60, CustomRateLimitFilterFactory.KeyType.IP)))
                                .circuitBreaker(config -> config.setName("auth-cb")))
                        .uri("lb://user-service"))

                // User Service Test endpoints
                .route("user-service-test", r -> r
                        .path("/api/users/test/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(50, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("user-test-cb")))
                        .uri("lb://user-service"))

                // User management - Admin only with moderate limits
                .route("user-service-management", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(20, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("user-mgmt-cb")))
                        .uri("lb://user-service"))

                // User Service Swagger Documentation
                .route("user-service-swagger-ui", r -> r
                        .path("/user-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/user-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://user-service"))

                .route("user-service-api-docs", r -> r
                        .path("/user-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/user-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://user-service"))

                // Product service - Higher limits for read operations
                .route("product-service-read", r -> r
                        .path("/api/products/**")
                        .and().method(HttpMethod.GET)
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(200, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("product-read-cb")))
                        .uri("lb://product-service"))

                // Product service - Lower limits for write operations
                .route("product-service-write", r -> r
                        .path("/api/products/**")
                        .and().method(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH)
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(50, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("product-write-cb")))
                        .uri("lb://product-service"))

                .route("product-service-front", r -> r
                        .path("/api/products/products/with-images**")
                        .filters(f -> f
                                .filter(customRateLimitFilterFactory.apply(createConfig(5, 60, CustomRateLimitFilterFactory.KeyType.IP)))
                                .circuitBreaker(config -> config.setName("auth-cb")))
                        .uri("lb://user-service"))

                // Product Service Swagger
                .route("product-service-swagger-ui", r -> r
                        .path("/product-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/product-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://product-service"))

                .route("product-service-api-docs", r -> r
                        .path("/product-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/product-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://product-service"))

                // Order service - Moderate limits
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(30, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("order-cb")))
                        .uri("lb://ORDER-SERVICE"))

                .route("order-service-swagger-ui", r -> r
                        .path("/order-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/order-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://ORDER-SERVICE"))

                .route("order-service-api-docs", r -> r
                        .path("/order-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/order-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://ORDER-SERVICE"))

                // Payment service - Very restrictive limits for security
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(tokenBucketRateLimitFilterFactory.apply(createTokenBucketConfig(3, 1, 10, 1)))
                                .circuitBreaker(config -> config.setName("payment-cb")))
                        .uri("lb://PAYMENT-SERVICE"))

                .route("payment-service-swagger-ui", r -> r
                        .path("/payment-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/payment-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://PAYMENT-SERVICE"))

                .route("payment-service-api-docs", r -> r
                        .path("/payment-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/payment-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://PAYMENT-SERVICE"))

                // Cart service - High limits for frequent operations
                .route("cart-service", r -> r
                        .path("/api/cart/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(50, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("cart-cb")))
                        .uri("lb://CART-SERVICE"))

                .route("cart-service-swagger-ui", r -> r
                        .path("/cart-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/cart-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://CART-SERVICE"))

                .route("cart-service-api-docs", r -> r
                        .path("/cart-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/cart-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://CART-SERVICE"))

                // Loyalty Service Routes
                .route("loyalty-service-api", r -> r
                        .path("/api/loyalty/**", "/loyalty/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(40, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("loyalty-cb")))
                        .uri("lb://LOYALTY-SERVICE"))

                .route("loyalty-service-swagger-ui", r -> r
                        .path("/loyalty-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/loyalty-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://LOYALTY-SERVICE"))

                .route("loyalty-service-api-docs", r -> r
                        .path("/loyalty-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/loyalty-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://LOYALTY-SERVICE"))

                // Notification Service Routes
                .route("notification-service-api", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(30, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("notification-cb")))
                        .uri("lb://NOTIFICATION-SERVICE"))

                .route("notification-service-swagger-ui", r -> r
                        .path("/notification-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/notification-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://NOTIFICATION-SERVICE"))

                .route("notification-service-api-docs", r -> r
                        .path("/notification-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/notification-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://NOTIFICATION-SERVICE"))

                // Shipping Service Routes (Go service)
                .route("shipping-service-api", r -> r
                        .path("/api/shipping/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
                                .filter(customRateLimitFilterFactory.apply(createConfig(25, 60, CustomRateLimitFilterFactory.KeyType.USER)))
                                .circuitBreaker(config -> config.setName("shipping-cb")))
                        .uri("lb://SHIPPING-SERVICE"))

                .route("shipping-service-swagger-ui", r -> r
                        .path("/shipping-service/swagger-ui/**")
                        .filters(f -> f.rewritePath("/shipping-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
                        .uri("lb://SHIPPING-SERVICE"))

                .route("shipping-service-api-docs", r -> r
                        .path("/shipping-service/v3/api-docs/**")
                        .filters(f -> f.rewritePath("/shipping-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
                        .uri("lb://SHIPPING-SERVICE"))

                .build();
    }

    private CustomRateLimitFilterFactory.Config createConfig(int limit, int windowSeconds, CustomRateLimitFilterFactory.KeyType keyType) {
        CustomRateLimitFilterFactory.Config config = new CustomRateLimitFilterFactory.Config();
        config.setLimit(limit);
        config.setWindowSizeInSeconds(windowSeconds);
        config.setKeyType(keyType);
        return config;
    }

    private TokenBucketRateLimitFilterFactory.Config createTokenBucketConfig(int capacity, int refillTokens, int refillInterval, int requestedTokens) {
        TokenBucketRateLimitFilterFactory.Config config = new TokenBucketRateLimitFilterFactory.Config();
        config.setBucketCapacity(capacity);
        config.setRefillTokens(refillTokens);
        config.setRefillIntervalSeconds(refillInterval);
        config.setRequestedTokens(requestedTokens);
        return config;
    }
}