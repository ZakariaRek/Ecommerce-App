package com.Ecommerce.Order_Service.Aspects;


import com.Ecommerce.Order_Service.Health.ShardingSphereMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Aspect for monitoring ShardingSphere operations
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ShardingSphereMonitoringAspect {

    private final ShardingSphereMetrics metrics;

    @Around("execution(* com.Ecommerce.Order_Service.Repositories.*.*(..)) || " +
            "execution(* com.Ecommerce.Order_Service.Services.*OrderService.*(..))")
    public Object monitorDatabaseOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String operation = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        try {
            Object result = joinPoint.proceed();

            Duration executionTime = Duration.between(start, Instant.now());

            // Record successful operation
            metrics.recordQuery(operation, determineShard(joinPoint.getArgs()), executionTime);

            log.debug("🔀 SHARDINGSPHERE: Operation {} completed in {}ms",
                    operation, executionTime.toMillis());

            return result;
        } catch (Exception e) {
            // Record error
            metrics.recordError(operation, e.getClass().getSimpleName());

            log.error("🔀 SHARDINGSPHERE: Operation {} failed", operation, e);
            throw e;
        }
    }

    private String determineShard(Object[] args) {
        // Analyze arguments to determine if this is likely a single-shard or cross-shard operation
        if (args.length > 0) {
            Object firstArg = args[0];
            if (firstArg != null && firstArg.toString().contains("user")) {
                return "single-shard";
            }
        }

        // Default assumption for operations without clear shard targeting
        return "cross-shard";
    }
}