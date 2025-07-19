package com.Ecommerce.Loyalty_Service.Config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for loyalty service factory settings
 */
@Configuration
@ConfigurationProperties(prefix = "loyalty.factory")
@Slf4j
public class LoyaltyFactoryConfig {

    private boolean createDefaultData = true;
    private boolean createTestData = false;

    public boolean isCreateDefaultData() {
        return createDefaultData;
    }

    public void setCreateDefaultData(boolean createDefaultData) {
        this.createDefaultData = createDefaultData;
    }

    public boolean isCreateTestData() {
        return createTestData;
    }

    public void setCreateTestData(boolean createTestData) {
        this.createTestData = createTestData;
    }
}