package com.Ecommerce.Gateway_Service.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationFilterFactory.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public JwtAuthenticationFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Skip authentication for public endpoints
            if (isPublicEndpoint(path)) {
                return chain.filter(exchange);
            }

            // Extract JWT token from cookies or Authorization header
            String token = extractToken(request);

            if (token == null) {
                return handleUnauthorized(exchange, "Missing authentication token");
            }

            // Validate JWT token
            if (!jwtUtil.validateToken(token)) {
                return handleUnauthorized(exchange, "Invalid or expired token");
            }

            // Extract user information
            String username = jwtUtil.extractUsername(token);
            List<String> roles = jwtUtil.extractRoles(token);

            // Check role-based access
            if (!hasRequiredRole(path, roles)) {
                return handleForbidden(exchange, "Insufficient permissions");
            }

            // Add user information to headers for downstream services
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", username)
                    .header("X-User-Roles", String.join(",", roles))
                    .header("X-Authenticated-User", username)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private String extractToken(ServerHttpRequest request) {
        // First try to get from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Then try to get from cookies
        String cookieHeader = request.getHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=");
                if (parts.length == 2 && "user-service".equals(parts[0])) {
                    return parts[1];
                }
            }
        }

        return null;
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/auth/signin") ||
                path.contains("/auth/signup") ||
                path.contains("/api/test/all") ||
                path.contains("/swagger") ||
                path.contains("/api-docs") ||
                path.contains("/health") ||
                path.contains("/actuator");
    }

    private boolean hasRequiredRole(String path, List<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }

        // Define role-based access rules based on your services
        if (path.contains("/api/test/admin")) {
            return userRoles.contains("ROLE_ADMIN");
        }
        if (path.contains("/api/test/mod")) {
            return userRoles.contains("ROLE_MODERATOR") || userRoles.contains("ROLE_ADMIN");
        }
        if (path.contains("/api/users") && !path.contains("/auth")) {
            return userRoles.contains("ROLE_ADMIN"); // User management requires admin
        }
        if (path.contains("/api/orders")) {
            return userRoles.contains("ROLE_USER") || userRoles.contains("ROLE_ADMIN");
        }
        if (path.contains("/api/products")) {
            // Products can be viewed by anyone authenticated, but creation requires admin
            if (path.contains("POST") || path.contains("PUT") || path.contains("DELETE")) {
                return userRoles.contains("ROLE_ADMIN");
            }
            return true; // Read access for all authenticated users
        }

        // Default: allow if user has any valid role
        return userRoles.stream().anyMatch(role ->
                role.equals("ROLE_USER") || role.equals("ROLE_MODERATOR") || role.equals("ROLE_ADMIN"));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    private Mono<Void> handleForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\":\"Forbidden\",\"message\":\"%s\"}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {
        // Configuration properties if needed
    }
}