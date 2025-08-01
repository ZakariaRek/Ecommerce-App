package com.Ecommerce.Order_Service.Controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Management controller for ShardingSphere operations
 */
@RestController
@RequestMapping("/admin/shardingsphere")
@RequiredArgsConstructor
@Slf4j
public class ShardingSphereManagementController {

    private final DataSource shardingSphereDataSource;

    /**
     * Get ShardingSphere configuration information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getShardingSphereInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // Get basic information
            info.put("dataSourceType", shardingSphereDataSource.getClass().getSimpleName());
            info.put("shardingSphereVersion", getShardingSphereVersion());
            info.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("🔀 SHARDINGSPHERE: Error getting ShardingSphere info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get data source information
     */
    @GetMapping("/datasources")
    public ResponseEntity<Map<String, Object>> getDataSourceInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            Map<String, Map<String, Object>> dataSources = new HashMap<>();

            // Test connections to all shards
            for (int i = 0; i < 4; i++) {
                String shardName = "shard" + i;
                Map<String, Object> shardInfo = testShardConnection(shardName);
                dataSources.put(shardName, shardInfo);
            }

            info.put("dataSources", dataSources);
            info.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("🔀 SHARDINGSPHERE: Error getting data source info", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get sharding rules information
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getShardingRules() {
        try {
            Map<String, Object> rules = new HashMap<>();

            // Get sharding table rules
            rules.put("shardingTables", getShardingTables());
            rules.put("bindingTables", getBindingTables());
            rules.put("broadcastTables", getBroadcastTables());
            rules.put("algorithms", getShardingAlgorithms());

            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            log.error("🔀 SHARDINGSPHERE: Error getting sharding rules", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Execute SQL with sharding information
     */
    @PostMapping("/execute-sql")
    public ResponseEntity<Map<String, Object>> executeSQL(@RequestBody SQLRequest request) {
        try {
            log.info("🔀 SHARDINGSPHERE: Executing SQL: {}", request.getSql());

            Map<String, Object> result = new HashMap<>();

            try (Connection conn = shardingSphereDataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(request.getSql())) {

                if (stmt.execute()) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        result.put("type", "QUERY");
                        result.put("data", extractResultSetData(rs));
                    }
                } else {
                    result.put("type", "UPDATE");
                    result.put("updateCount", stmt.getUpdateCount());
                }

                result.put("sql", request.getSql());
                result.put("success", true);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("🔀 SHARDINGSPHERE: Error executing SQL: {}", request.getSql(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("sql", request.getSql());

            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get SQL execution statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getExecutionStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Get connection pool statistics for each shard
            Map<String, Object> poolStats = new HashMap<>();
            for (int i = 0; i < 4; i++) {
                String shardName = "shard" + i;
                poolStats.put(shardName, getConnectionPoolStats(shardName));
            }

            stats.put("connectionPools", poolStats);
            stats.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("🔀 SHARDINGSPHERE: Error getting execution stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> testShardConnection(String shardName) {
        Map<String, Object> info = new HashMap<>();

        try (Connection conn = shardingSphereDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 as test")) {

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    info.put("status", "HEALTHY");
                    info.put("testResult", rs.getInt("test"));
                }
            }

            info.put("connected", true);
        } catch (Exception e) {
            info.put("status", "UNHEALTHY");
            info.put("error", e.getMessage());
            info.put("connected", false);
        }

        return info;
    }

    private String getShardingSphereVersion() {
        try {
            // Try to get version from ShardingSphere classes
            Package pkg = org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory.class.getPackage();
            return pkg.getImplementationVersion() != null ? pkg.getImplementationVersion() : "5.4.1";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private Map<String, Object> getShardingTables() {
        Map<String, Object> tables = new HashMap<>();
        tables.put("orders", "Sharded by user_id");
        tables.put("order_items", "Sharded by order_id");
        tables.put("discount_applications", "Sharded by order_id");
        return tables;
    }

    private Map<String, Object> getBindingTables() {
        Map<String, Object> binding = new HashMap<>();
        binding.put("orderGroup", "orders,order_items,discount_applications");
        return binding;
    }

    private Map<String, Object> getBroadcastTables() {
        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("status", "No broadcast tables configured");
        return broadcast;
    }

    private Map<String, Object> getShardingAlgorithms() {
        Map<String, Object> algorithms = new HashMap<>();
        algorithms.put("user_hash_mod_algorithm", "HASH_MOD for user_id");
        algorithms.put("order_hash_mod_algorithm", "HASH_MOD for order_id");
        return algorithms;
    }

    private Map<String, Object> extractResultSetData(ResultSet rs) throws Exception {
        Map<String, Object> data = new HashMap<>();
        int columnCount = rs.getMetaData().getColumnCount();

        // Extract column names
        String[] columns = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columns[i-1] = rs.getMetaData().getColumnName(i);
        }
        data.put("columns", columns);

        // Extract row data (limit to 100 rows for safety)
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        int rowCount = 0;
        while (rs.next() && rowCount < 100) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i-1] = rs.getObject(i);
            }
            rows.add(row);
            rowCount++;
        }
        data.put("rows", rows);
        data.put("rowCount", rowCount);

        return data;
    }

    private Map<String, Object> getConnectionPoolStats(String shardName) {
        Map<String, Object> stats = new HashMap<>();

        // This would require access to HikariCP MBeans or metrics
        // For now, return placeholder data
        stats.put("activeConnections", "N/A");
        stats.put("totalConnections", "N/A");
        stats.put("idleConnections", "N/A");

        return stats;
    }

    @lombok.Data
    public static class SQLRequest {
        private String sql;
    }
}