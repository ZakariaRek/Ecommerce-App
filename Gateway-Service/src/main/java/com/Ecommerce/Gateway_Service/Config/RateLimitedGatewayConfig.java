//package com.Ecommerce.Gateway_Service.Config;
//
//import com.Ecommerce.Gateway_Service.Security.CustomRateLimitFilterFactory;
//import com.Ecommerce.Gateway_Service.Security.JwtAuthenticationFilterFactory;
//import com.Ecommerce.Gateway_Service.Security.TokenBucketRateLimitFilterFactory;
//import org.springframework.cloud.gateway.route.RouteLocator;
//import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RateLimitedGatewayConfig {
//
//    private final JwtAuthenticationFilterFactory jwtAuthenticationFilterFactory;
//    private final CustomRateLimitFilterFactory customRateLimitFilterFactory;
//    private final TokenBucketRateLimitFilterFactory tokenBucketRateLimitFilterFactory;
//
//    public RateLimitedGatewayConfig(
//            JwtAuthenticationFilterFactory jwtAuthenticationFilterFactory,
//            CustomRateLimitFilterFactory customRateLimitFilterFactory,
//            TokenBucketRateLimitFilterFactory tokenBucketRateLimitFilterFactory) {
//        this.jwtAuthenticationFilterFactory = jwtAuthenticationFilterFactory;
//        this.customRateLimitFilterFactory = customRateLimitFilterFactory;
//        this.tokenBucketRateLimitFilterFactory = tokenBucketRateLimitFilterFactory;
//    }
//
//    @Bean
//    public RouteLocator rateLimitedRoutes(RouteLocatorBuilder builder) {
//        return builder.routes()
//                // Authentication endpoints - More restrictive rate limiting
//                .route("user-service-auth", r -> r
//                        .path("/api/users/auth/**")
//                        .filters(f -> f
//                                .filter(customRateLimitFilterFactory.apply(createConfig(5, 60, CustomRateLimitFilterFactory.KeyType.IP)))
//                                .circuitBreaker(config -> config.setName("auth-cb")))
//                        .uri("lb://USER-SERVICE"))
//
//                // User management - Admin only with moderate limits
//                .route("user-service-management", r -> r
//                        .path("/api/users/api/users/**")
//                        .filters(f -> f
//                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
//                                .filter(customRateLimitFilterFactory.apply(createConfig(20, 60, CustomRateLimitFilterFactory.KeyType.USER)))
//                                .circuitBreaker(config -> config.setName("user-mgmt-cb")))
//                        .uri("lb://USER-SERVICE"))
//
//                // Product service - Higher limits for read operations
//                .route("product-service-read", r -> r
//                        .path("/api/products/**")
//                        .and().method("GET")
//                        .filters(f -> f
//                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
//                                .filter(customRateLimitFilterFactory.apply(createConfig(100, 60, CustomRateLimitFilterFactory.KeyType.USER)))
//                                .circuitBreaker(config -> config.setName("product-read-cb")))
//                        .uri("lb://PRODUCT-SERVICE"))
//
//                // Product service - Lower limits for write operations
//                .route("product-service-write", r -> r
//                        .path("/api/products/**")
//                        .and().method("POST", "PUT", "DELETE", "PATCH")
//                        .filters(f -> f
//                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
//                                .filter(customRateLimitFilterFactory.apply(createConfig(10, 60, CustomRateLimitFilterFactory.KeyType.USER)))
//                                .circuitBreaker(config -> config.setName("product-write-cb")))
//                        .uri("lb://PRODUCT-SERVICE"))
//
//                // Order service - Moderate limits
//                .route("order-service", r -> r
//                        .path("/api/orders/**")
//                        .filters(f -> f
//                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
//                                .filter(customRateLimitFilterFactory.apply(createConfig(30, 60, CustomRateLimitFilterFactory.KeyType.USER)))
//                                .circuitBreaker(config -> config.setName("order-cb")))
//                        .uri("lb://ORDER-SERVICE"))
//
//                // Payment service - Very restrictive limits for security
//                .route("payment-service", r -> r
//                        .path("/api/payments/**")
//                        .filters(f -> f
//                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
//                                .filter(tokenBucketRateLimitFilterFactory.apply(createTokenBucketConfig(3, 1, 10, 1)))
//                                .circuitBreaker(config -> config.setName("payment-cb")))
//                        .uri("lb://PAYMENT-SERVICE"))
//
//                // Cart service - High limits for frequent operations
//                .route("cart-service", r -> r
//                        .path("/api/cart/**")
//                        .filters(f -> f
//                                .filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config()))
//                                .filter(customRateLimitFilterFactory.apply(createConfig(50, 60, CustomRateLimitFilterFactory.KeyType.USER)))
//                                .circuitBreaker(config -> config.setName("cart-cb")))
//                        .uri("lb://CART-SERVICE"))
//
//                .build();
//    }
//
//    private CustomRateLimitFilterFactory.Config createConfig(int limit, int windowSeconds, CustomRateLimitFilterFactory.KeyType keyType) {
//        CustomRateLimitFilterFactory.Config config = new CustomRateLimitFilterFactory.Config();
//        config.setLimit(limit);
//        config.setWindowSizeInSeconds(windowSeconds);
//        config.setKeyType(keyType);
//        return config;
//    }
//
//    private TokenBucketRateLimitFilterFactory.Config createTokenBucketConfig(int capacity, int refillTokens, int refillInterval, int requestedTokens) {
//        TokenBucketRateLimitFilterFactory.Config config = new TokenBucketRateLimitFilterFactory.Config();
//        config.setBucketCapacity(capacity);
//        config.setRefillTokens(refillTokens);
//        config.setRefillIntervalSeconds(refillInterval);
//        config.setRequestedTokens(requestedTokens);
//        return config;
//    }
//}
