package com.Ecommerce.Gateway_Service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class GatewaySwaggerConfig {

    @Bean
    public RouterFunction<ServerResponse> routeSwaggerResources() {
        return RouterFunctions
                .route(RequestPredicates.GET("/swagger-docs"),
                        req -> ServerResponse.status(HttpStatus.FOUND)
                                .header("Location", "/swagger-resources")
                                .build());
    }
}