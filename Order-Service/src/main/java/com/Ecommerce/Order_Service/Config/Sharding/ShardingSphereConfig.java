package com.Ecommerce.Order_Service.Config.Sharding;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ShardingSphere Configuration for Order Service
 */
@Configuration
public class ShardingSphereConfig {

    /**
     * Create ShardingSphere DataSource from YAML configuration
     */
    @Bean
    @Primary
    public DataSource shardingSphereDataSource() throws SQLException, IOException {
        // Load ShardingSphere configuration from YAML file using Spring's ClassPathResource
        File yamlFile = new ClassPathResource("shardingsphere-config.yaml").getFile();
        return YamlShardingSphereDataSourceFactory.createDataSource(yamlFile);
    }

    /**
     * Alternative: Programmatic configuration (if you prefer Java config over YAML)
     */
    //@Bean
    //@Primary
    public DataSource programmaticShardingSphereDataSource() throws SQLException {
        return createShardingSphereDataSource();
    }

    private DataSource createShardingSphereDataSource() throws SQLException {
        // Configure data sources
        Map<String, DataSource> dataSourceMap = createDataSourceMap();

        // Configure sharding rule
        ShardingRuleConfiguration shardingRuleConfig = createShardingRuleConfiguration();

        // Create ShardingSphere data source
        return org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory
                .createDataSource(dataSourceMap, java.util.Collections.singleton(shardingRuleConfig), new Properties());
    }

    private Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> dataSourceMap = new HashMap<>();

        // Create data sources for each shard
        dataSourceMap.put("shard0", createDataSource("jdbc:postgresql://localhost:5432/Order-service", "postgres", "yahyasd56"));
        dataSourceMap.put("shard1", createDataSource("jdbc:postgresql://localhost:5432/Order-service-shard-1", "postgres", "yahyasd56"));
        dataSourceMap.put("shard2", createDataSource("jdbc:postgresql://localhost:5432/Order-service-shard-2", "postgres", "yahyasd56"));
        dataSourceMap.put("shard3", createDataSource("jdbc:postgresql://localhost:5432/Order-service-shard-3", "postgres", "yahyasd56"));

        return dataSourceMap;
    }

    private DataSource createDataSource(String url, String username, String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        return dataSource;
    }

    private ShardingRuleConfiguration createShardingRuleConfiguration() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        // Configure sharding tables
        shardingRuleConfig.getTables().add(createOrderTableRuleConfiguration());
        shardingRuleConfig.getTables().add(createOrderItemTableRuleConfiguration());
        shardingRuleConfig.getTables().add(createDiscountApplicationTableRuleConfiguration());

        // Configure sharding algorithms
        shardingRuleConfig.getShardingAlgorithms().put("user-hash-mod", createUserHashModAlgorithm());
        shardingRuleConfig.getShardingAlgorithms().put("order-hash-mod", createOrderHashModAlgorithm());

        return shardingRuleConfig;
    }

    private ShardingTableRuleConfiguration createOrderTableRuleConfiguration() {
        ShardingTableRuleConfiguration result = new ShardingTableRuleConfiguration("orders", "shard${0..3}.orders");

        // Database sharding strategy (by user_id)
        result.setDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("user_id", "user-hash-mod"));

        // Table sharding strategy (optional - could also shard tables within each database)
        // result.setTableShardingStrategy(new StandardShardingStrategyConfiguration("id", "order-hash-mod"));

        return result;
    }

    private ShardingTableRuleConfiguration createOrderItemTableRuleConfiguration() {
        ShardingTableRuleConfiguration result = new ShardingTableRuleConfiguration("order_items", "shard${0..3}.order_items");

        // Shard by order_id to keep order items with their parent order
        result.setDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("order_id", "order-hash-mod"));

        return result;
    }

    private ShardingTableRuleConfiguration createDiscountApplicationTableRuleConfiguration() {
        ShardingTableRuleConfiguration result = new ShardingTableRuleConfiguration("discount_applications", "shard${0..3}.discount_applications");

        // Shard by order_id to keep discount applications with their parent order
        result.setDatabaseShardingStrategy(new StandardShardingStrategyConfiguration("order_id", "order-hash-mod"));

        return result;
    }

    private AlgorithmConfiguration createUserHashModAlgorithm() {
        Properties props = new Properties();
        props.setProperty("sharding-count", "4");
        return new AlgorithmConfiguration("USER_BASED", props);
    }

    private AlgorithmConfiguration createOrderHashModAlgorithm() {
        Properties props = new Properties();
        props.setProperty("sharding-count", "4");
        return new AlgorithmConfiguration("ORDER_BASED", props);
    }
}