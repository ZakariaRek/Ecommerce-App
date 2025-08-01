package com.Ecommerce.Order_Service.Health;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for ShardingSphere data sources
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShardingSphereHealthIndicator implements HealthIndicator {

    private final DataSource shardingSphereDataSource;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // Test basic connectivity
            boolean isHealthy = testConnectivity();
            details.put("shardingSphere", isHealthy ? "UP" : "DOWN");

            // Test each logical shard
            Map<String, String> shardHealth = testShardHealth();
            details.put("shards", shardHealth);

            // Get connection pool information
            details.put("connectionPool", getConnectionPoolInfo());

            // Overall health
            boolean allShardsHealthy = shardHealth.values().stream()
                    .allMatch("UP"::equals);

            details.put("overallStatus", isHealthy && allShardsHealthy ? "UP" : "DOWN");

            return isHealthy && allShardsHealthy ?
                    Health.up().withDetails(details).build() :
                    Health.down().withDetails(details).build();

        } catch (Exception e) {
            log.error("🔀 SHARDINGSPHERE: Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("shardingSphere", "DOWN")
                    .build();
        }
    }

    private boolean testConnectivity() {
        try (Connection conn = shardingSphereDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (Exception e) {
            log.warn("🔀 SHARDINGSPHERE: Connectivity test failed", e);
            return false;
        }
    }

    private Map<String, String> testShardHealth() {
        Map<String, String> shardHealth = new HashMap<>();

        // Test each shard by executing a simple query that should hit each one
        for (int i = 0; i < 4; i++) {
            String shardKey = "shard" + i;
            try {
                // Execute a query that would route to this specific shard
                boolean healthy = testSpecificShard(i);
                shardHealth.put(shardKey, healthy ? "UP" : "DOWN");
            } catch (Exception e) {
                log.warn("🔀 SHARDINGSPHERE: Shard {} health check failed", i, e);
                shardHealth.put(shardKey, "DOWN");
            }
        }

        return shardHealth;
    }

    private boolean testSpecificShard(int shardIndex) {
        try (Connection conn = shardingSphereDataSource.getConnection()) {
            // Generate a user ID that should route to the specific shard
            String testUserId = generateUserIdForShard(shardIndex);

            String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, testUserId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next(); // Just test that query executes
                }
            }
        } catch (Exception e) {
            log.debug("🔀 SHARDINGSPHERE: Shard {} test query failed", shardIndex, e);
            return false;
        }
    }

    private String generateUserIdForShard(int targetShard) {
        // Generate a UUID that should hash to the target shard
        // This is a simplified approach - in practice, you might need more sophisticated logic
        for (int i = 0; i < 1000; i++) {
            String testId = java.util.UUID.randomUUID().toString();
            if (Math.abs(testId.hashCode()) % 4 == targetShard) {
                return testId;
            }
        }
        // Fallback
        return "test-user-shard-" + targetShard;
    }

    private Map<String, Object> getConnectionPoolInfo() {
        Map<String, Object> poolInfo = new HashMap<>();

        try {
            // This would require access to the underlying connection pools
            // For now, provide basic information
            poolInfo.put("dataSourceType", shardingSphereDataSource.getClass().getSimpleName());
            poolInfo.put("status", "Available");
        } catch (Exception e) {
            poolInfo.put("status", "Error: " + e.getMessage());
        }

        return poolInfo;
    }
}
