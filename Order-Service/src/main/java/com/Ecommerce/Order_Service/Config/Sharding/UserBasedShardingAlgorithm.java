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
 * Custom sharding algorithm for user-based sharding
 * Supports both String and UUID user IDs
 */
@Slf4j
public class UserBasedShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private Properties props;
    private int shardingCount;

    @Override
    public void init(Properties props) {
        this.props = props;
        this.shardingCount = Integer.parseInt(props.getProperty("sharding-count", "4"));
        log.info("🔀 SHARDINGSPHERE: UserBasedShardingAlgorithm initialized with {} shards", shardingCount);
    }

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {
        Object value = shardingValue.getValue();
        String logicalTableName = shardingValue.getLogicTableName();
        String columnName = shardingValue.getColumnName();

        log.debug("🔀 SHARDINGSPHERE: Sharding {} by {} = {}", logicalTableName, columnName, value);

        if (value == null) {
            log.warn("🔀 SHARDINGSPHERE: Null sharding value, using default shard");
            return getShardName(availableTargetNames, 0);
        }

        int shardIndex = calculateShardIndex(value);
        String targetName = getShardName(availableTargetNames, shardIndex);

        log.debug("🔀 SHARDINGSPHERE: Routed {} to shard {}", value, targetName);
        return targetName;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Comparable<?>> shardingValue) {
        // For range queries, return all available shards
        log.debug("🔀 SHARDINGSPHERE: Range sharding for {}, returning all shards", shardingValue.getLogicTableName());
        return availableTargetNames;
    }

    private int calculateShardIndex(Object value) {
        try {
            String stringValue = convertToString(value);

            // Use consistent hashing with MD5 for better distribution
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(stringValue.getBytes());

            // Convert first 4 bytes to int
            int hashInt = 0;
            for (int i = 0; i < 4; i++) {
                hashInt = (hashInt << 8) | (hash[i] & 0xFF);
            }

            return Math.abs(hashInt) % shardingCount;

        } catch (NoSuchAlgorithmException e) {
            log.warn("🔀 SHARDINGSPHERE: MD5 not available, falling back to simple hash", e);
            return Math.abs(value.hashCode()) % shardingCount;
        }
    }

    private String convertToString(Object value) {
        if (value instanceof UUID) {
            return value.toString();
        } else if (value instanceof String) {
            // Handle MongoDB ObjectId format
            String str = (String) value;
            if (str.length() == 24 && str.matches("[0-9a-fA-F]+")) {
                // MongoDB ObjectId - use as is
                return str;
            }
            // Try to parse as UUID
            try {
                UUID.fromString(str);
                return str;
            } catch (IllegalArgumentException e) {
                // Not a UUID, use as string
                return str;
            }
        } else {
            return value.toString();
        }
    }

    private String getShardName(Collection<String> availableTargetNames, int shardIndex) {
        String targetShard = "shard" + shardIndex;

        // Find the matching shard name
        for (String targetName : availableTargetNames) {
            if (targetName.contains(targetShard) || targetName.equals(targetShard)) {
                return targetName;
            }
        }

        // Fallback: return first available shard
        log.warn("🔀 SHARDINGSPHERE: Target shard {} not found, using fallback", targetShard);
        return availableTargetNames.iterator().next();
    }

    @Override
    public String getType() {
        return "USER_BASED";
    }

}