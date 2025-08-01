package com.Ecommerce.Order_Service.Health;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom metrics for ShardingSphere operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShardingSphereMetrics {

    private final MeterRegistry meterRegistry;

    private Counter totalQueries;
    private Counter crossShardQueries;
    private Counter singleShardQueries;
    private Timer queryExecutionTime;
    private Timer shardResolutionTime;

    private final ConcurrentMap<String, Counter> shardSpecificCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> operationTimers = new ConcurrentHashMap<>();

    @PostConstruct
    public void initMetrics() {
        totalQueries = Counter.builder("shardingsphere.queries.total")
                .description("Total number of queries executed through ShardingSphere")
                .register(meterRegistry);

        crossShardQueries = Counter.builder("shardingsphere.queries.cross_shard")
                .description("Number of cross-shard queries executed")
                .register(meterRegistry);

        singleShardQueries = Counter.builder("shardingsphere.queries.single_shard")
                .description("Number of single-shard queries executed")
                .register(meterRegistry);

        queryExecutionTime = Timer.builder("shardingsphere.query.execution_time")
                .description("Time taken to execute queries")
                .register(meterRegistry);

        shardResolutionTime = Timer.builder("shardingsphere.shard.resolution_time")
                .description("Time taken to resolve shard routing")
                .register(meterRegistry);

        log.info("🔀 SHARDINGSPHERE: Metrics initialized successfully");
    }

    public void recordQuery(String operation, String shardInfo, Duration executionTime) {
        totalQueries.increment();

        if (shardInfo.contains("cross-shard")) {
            crossShardQueries.increment();
        } else {
            singleShardQueries.increment();
        }

        queryExecutionTime.record(executionTime);

        // Record shard-specific metrics
        getShardCounter(shardInfo).increment();
        getOperationTimer(operation).record(executionTime);
    }

    public void recordShardResolution(String userId, String shardId, Duration resolutionTime) {
        shardResolutionTime.record(resolutionTime);

        Counter shardResolutionCounter = Counter.builder("shardingsphere.shard.resolutions")
                .tag("shard", shardId)
                .description("Number of shard resolutions per shard")
                .register(meterRegistry);

        shardResolutionCounter.increment();
    }

    public void recordError(String operation, String errorType) {
        Counter errorCounter = Counter.builder("shardingsphere.errors")
                .tag("operation", operation)
                .tag("error_type", errorType)
                .description("Number of errors in ShardingSphere operations")
                .register(meterRegistry);

        errorCounter.increment();
    }

    private Counter getShardCounter(String shardInfo) {
        return shardSpecificCounters.computeIfAbsent(shardInfo, shard ->
                Counter.builder("shardingsphere.shard.queries")
                        .tag("shard", shard)
                        .description("Queries per shard")
                        .register(meterRegistry));
    }

    private Timer getOperationTimer(String operation) {
        return operationTimers.computeIfAbsent(operation, op ->
                Timer.builder("shardingsphere.operation.time")
                        .tag("operation", op)
                        .description("Execution time per operation type")
                        .register(meterRegistry));
    }
}