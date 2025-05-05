package com.Ecommerce.Order_Service.Config;

import com.Ecommerce.Order_Service.Listeners.OrderEntityListener;
import com.Ecommerce.Order_Service.Listeners.OrderItemEntityListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;

/**
 * Configuration for JPA entity listeners
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.Ecommerce.Order_Service.Repositories")
public class JPAListenerConfig {

    /**
     * Register the Order entity listener
     */
    @Bean
    public OrderEntityListener orderEntityListener() {
        return new OrderEntityListener();
    }

    /**
     * Register the OrderItem entity listener
     */
    @Bean
    public OrderItemEntityListener orderItemEntityListener() {
        return new OrderItemEntityListener();
    }

    /**
     * Configure transaction manager
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}