package com.Ecommerce.Order_Service.Config.Sharding;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Register custom sharding algorithms with ShardingSphere
 */
@Configuration
public class ShardingAlgorithmProvider {

    @Bean
    public UserBasedShardingAlgorithm userBasedShardingAlgorithm() {
        return new UserBasedShardingAlgorithm();
    }

    @Bean
    public OrderBasedShardingAlgorithm orderBasedShardingAlgorithm() {
        return new OrderBasedShardingAlgorithm();
    }
}