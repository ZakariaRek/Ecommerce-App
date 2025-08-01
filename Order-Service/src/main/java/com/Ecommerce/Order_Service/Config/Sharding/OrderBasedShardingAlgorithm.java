package com.Ecommerce.Order_Service.Config.Sharding;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

/**
 * Custom sharding algorithm for order-based sharding
 * Used for related entities like order_items and discount_applications
 */
@Slf4j
public class OrderBasedShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private Properties props;
    private int shardingCount;

    @Override
    public void init(Properties props) {
        this.props = props;
        this.shardingCount = Integer.parseInt(props.getProperty("sharding-count", "4"));
        log.info("🔀 SHARDINGSPHERE: OrderBasedShardingAlgorithm initialized with {} shards", shardingCount);
    }

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {
        Object value = shardingValue.getValue();
        String logicalTableName = shardingValue.getLogicTableName();
        String columnName = shardingValue.getColumnName();

        log.debug("🔀 SHARDINGSPHERE: Order-based sharding {} by {} = {}", logicalTableName, columnName, value);

        if (value == null) {
            log.warn("🔀 SHARDINGSPHERE: Null order ID, using default shard");
            return getShardName(availableTargetNames, 0);
        }

        int shardIndex = calculateShardIndex(value);
        String targetName = getShardName(availableTargetNames, shardIndex);

        log.debug("🔀 SHARDINGSPHERE: Routed order {} to shard {}", value, targetName);
        return targetName;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Comparable<?>> shardingValue) {
        // For range queries on orders, might need to search all shards
        log.debug("🔀 SHARDINGSPHERE: Range sharding for orders, returning all shards");
        return availableTargetNames;
    }

    private int calculateShardIndex(Object value) {
        try {
            String stringValue = value.toString();

            // Use MD5 for consistent hashing
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(stringValue.getBytes());

            // Convert first 4 bytes to int
            int hashInt = 0;
            for (int i = 0; i < 4; i++) {
                hashInt = (hashInt << 8) | (hash[i] & 0xFF);
            }

            return Math.abs(hashInt) % shardingCount;

        } catch (NoSuchAlgorithmException e) {
            log.warn("🔀 SHARDINGSPHERE: MD5 not available for order sharding, falling back to simple hash", e);
            return Math.abs(value.hashCode()) % shardingCount;
        }
    }

    private String getShardName(Collection<String> availableTargetNames, int shardIndex) {
        String targetShard = "shard" + shardIndex;

        for (String targetName : availableTargetNames) {
            if (targetName.contains(targetShard) || targetName.equals(targetShard)) {
                return targetName;
            }
        }

        // Fallback
        return availableTargetNames.iterator().next();
    }

    @Override
    public String getType() {
        return "ORDER_BASED";
    }

}