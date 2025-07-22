package com.Ecommerce.Order_Service.Config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
//        return builder
//                .setConnectTimeout(Duration.ofSeconds(10))
//                .setReadTimeout(Duration.ofSeconds(30))
//                .requestFactory(SimpleClientHttpRequestFactory.class)
//                .build();
        return builder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(10_000); // 10 seconds
                    factory.setReadTimeout(30_000);   // 30 seconds
                    return factory;
                })
                .build();
    }
}