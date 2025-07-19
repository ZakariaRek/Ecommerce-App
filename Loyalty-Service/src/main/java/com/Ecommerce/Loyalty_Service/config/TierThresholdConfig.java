package com.Ecommerce.Loyalty_Service.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for membership tier thresholds
 */
@Configuration
@ConfigurationProperties(prefix = "loyalty.tier")
public class TierThresholdConfig {

    private int bronzeThreshold = 0;      // 0-499 points
    private int silverThreshold = 500;    // 500-1999 points
    private int goldThreshold = 2000;     // 2000-4999 points
    private int platinumThreshold = 5000; // 5000-9999 points
    private int diamondThreshold = 10000; // 10000+ points

    // Getters and setters
    public int getBronzeThreshold() { return bronzeThreshold; }
    public void setBronzeThreshold(int bronzeThreshold) { this.bronzeThreshold = bronzeThreshold; }

    public int getSilverThreshold() { return silverThreshold; }
    public void setSilverThreshold(int silverThreshold) { this.silverThreshold = silverThreshold; }

    public int getGoldThreshold() { return goldThreshold; }
    public void setGoldThreshold(int goldThreshold) { this.goldThreshold = goldThreshold; }

    public int getPlatinumThreshold() { return platinumThreshold; }
    public void setPlatinumThreshold(int platinumThreshold) { this.platinumThreshold = platinumThreshold; }

    public int getDiamondThreshold() { return diamondThreshold; }
    public void setDiamondThreshold(int diamondThreshold) { this.diamondThreshold = diamondThreshold; }
}

