package com.Ecommerce.Cart.Service.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configure content negotiation to always default to JSON
     * This helps when clients don't specify an Accept header
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .useRegisteredExtensionsOnly(false);
    }

    /**
     * Configure HTTP message converters to handle various content types
     * This ensures proper handling of JSON regardless of Accept headers
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Create a custom Jackson converter that accepts all media types
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();

        // Make this converter handle all media types
        jacksonConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));

        // Ensure proper encoding
        jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);

        // Add at the beginning to give it priority
        converters.add(0, jacksonConverter);
    }
}