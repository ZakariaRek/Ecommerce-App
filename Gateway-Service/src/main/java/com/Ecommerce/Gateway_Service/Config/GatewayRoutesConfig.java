//package com.Ecommerce.Gateway_Service.Config;
//
//import com.Ecommerce.Gateway_Service.Security.JwtAuthenticationFilterFactory;
//import org.springframework.cloud.gateway.route.RouteLocator;
//import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class GatewayRoutesConfig {
//
//    private final JwtAuthenticationFilterFactory jwtAuthenticationFilterFactory;
//
//    public GatewayRoutesConfig(JwtAuthenticationFilterFactory jwtAuthenticationFilterFactory) {
//        this.jwtAuthenticationFilterFactory = jwtAuthenticationFilterFactory;
//    }
//
//    @Bean
//    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//        return builder.routes()
//                // User Service Routes (Authentication & User Management)
//                .route("user-service-auth", r -> r
//                        .path("/api/users/auth/**")
//                        .uri("lb://USER-SERVICE"))
//
//                .route("user-service-test", r -> r
//                        .path("/api/users/api/test/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://USER-SERVICE"))
//
//                .route("user-service-management", r -> r
//                        .path("/api/users/api/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://USER-SERVICE"))
//
//                // User Service Swagger Documentation
//                .route("user-service-swagger-ui", r -> r
//                        .path("/user-service/swagger-ui/**")
//                        .filters(f -> f.rewritePath("/user-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
//                        .uri("lb://USER-SERVICE"))
//
//                .route("user-service-api-docs", r -> r
//                        .path("/user-service/v3/api-docs/**")
//                        .filters(f -> f.rewritePath("/user-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
//                        .uri("lb://USER-SERVICE"))
//
//                // Product Service Routes
//                .route("product-service-api", r -> r
//                        .path("/api/products/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://PRODUCT-SERVICE"))
//
//                // Order Service Routes
//                .route("order-service-api", r -> r
//                        .path("/api/orders/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://ORDER-SERVICE"))
//
//                .route("order-service-swagger-ui", r -> r
//                        .path("/order-service/swagger-ui/**")
//                        .filters(f -> f.rewritePath("/order-service/swagger-ui/(?<segment>.*)", "/swagger-ui/${segment}"))
//                        .uri("lb://ORDER-SERVICE"))
//
//                .route("order-service-api-docs", r -> r
//                        .path("/order-service/v3/api-docs/**")
//                        .filters(f -> f.rewritePath("/order-service/v3/api-docs/(?<segment>.*)", "/v3/api-docs/${segment}"))
//                        .uri("lb://ORDER-SERVICE"))
//
//                // Cart Service Routes
//                .route("cart-service-api", r -> r
//                        .path("/api/cart/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://CART-SERVICE"))
//
//                // Loyalty Service Routes
//                .route("loyalty-service-api", r -> r
//                        .path("/api/loyalty/**", "/loyalty/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://LOYALTY-SERVICE"))
//
//                // Notification Service Routes
//                .route("notification-service-api", r -> r
//                        .path("/api/notifications/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://NOTIFICATION-SERVICE"))
//
//                // Payment Service Routes (Go service)
//                .route("payment-service-api", r -> r
//                        .path("/api/payments/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://PAYMENT-SERVICE"))
//
//                // Shipping Service Routes (Go service)
//                .route("shipping-service-api", r -> r
//                        .path("/api/shipping/**")
//                        .filters(f -> f.filter(jwtAuthenticationFilterFactory.apply(new JwtAuthenticationFilterFactory.Config())))
//                        .uri("lb://SHIPPING-SERVICE"))
//
//                .build();
//    }
//}