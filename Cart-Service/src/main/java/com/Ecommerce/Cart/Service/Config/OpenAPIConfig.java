package com.Ecommerce.Cart.Service.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${spring.application.name:Cart Service}")
    private String applicationName;

    @Bean
    public OpenAPI cartServiceOpenAPI() {
        return new OpenAPI()
                .openapi("3.0.1")  // Add this line to explicitly set the OpenAPI version
                .info(new Info()
                        .title(applicationName + " API")
                        .description("Shopping Cart Service API for Ecommerce application")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Ecommerce Team")
                                .email("support@ecommerce.com"))
                        .license(new License()
                                .name("API License")
                                .url("https://www.example.com/licenses")))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL"),
                        new Server().url("http://localhost:8087").description("Local development server")
                ));
    }
}