package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Repositories.PointTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.hibernate.StaleObjectStateException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointTransactionService {
    private final PointTransactionRepository transactionRepository;
    private final CRMService crmService;

    /**
     * Original method - maintains backward compatibility
     */
    @Transactional
    public PointTransaction recordTransaction(UUID userId, TransactionType type, int points, String source) {
        return recordTransactionWithIdempotency(userId, type, points, source, null);
    }

    /**
     * Enhanced method with idempotency support and retry logic
     */
    @Transactional
    @Retryable(
            value = {
                    ObjectOptimisticLockingFailureException.class,
                    OptimisticLockingFailureException.class,
                    StaleObjectStateException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000)
    )
    public PointTransaction recordTransactionWithIdempotency(UUID userId, TransactionType type,
                                                             int points, String source, String idempotencyKey) {
        log.info("🔄 Recording {} transaction: {} points for user {} with key: {}",
                type, points, userId, idempotencyKey);

        try {
            // Check for duplicate processing if idempotency key is provided
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                Optional<PointTransaction> existingTransaction =
                        transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

                if (existingTransaction.isPresent()) {
                    log.info("⚠️ Duplicate transaction detected for key: {} - returning existing transaction",
                            idempotencyKey);
                    return existingTransaction.get();
                }
            }

            // Create new transaction - DON'T set ID manually
            PointTransaction transaction = new PointTransaction();
            // Remove this line: transaction.setId(UUID.randomUUID());
            transaction.setUserId(userId);
            transaction.setType(type);
            transaction.setPoints(points);
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setSource(source);
            transaction.setIdempotencyKey(idempotencyKey);

            // Update the CRM based on transaction type with retry logic
            if (type == TransactionType.EARN) {
                crmService.earnPoints(userId, points, source);
            } else if (type == TransactionType.REDEEM) {
                crmService.redeemPoints(userId, points);
            }

            // Get current balance after CRM update
            int currentBalance = crmService.getByUserId(userId).getTotalPoints();
            transaction.setBalance(currentBalance);

            // Save the transaction - JPA will generate ID and version automatically
            PointTransaction savedTransaction = transactionRepository.save(transaction);

            log.info("✅ Successfully recorded {} transaction of {} points for user {}, new balance: {}",
                    type, points, userId, currentBalance);

            return savedTransaction;

        } catch (OptimisticLockingFailureException | StaleObjectStateException e) {
            log.warn("⚠️ Optimistic locking failure for user {} - attempt will be retried. Error: {}",
                    userId, e.getMessage());
            throw e; // Let @Retryable handle the retry
        } catch (Exception e) {
            log.error("❌ Unexpected error recording transaction for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Recover method when all retries are exhausted for optimistic locking failures
     */
    @Recover
    public PointTransaction recoverFromOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex,
                                                                UUID userId, TransactionType type, int points,
                                                                String source, String idempotencyKey) {
        return handleRecovery(ex, userId, type, points, source, idempotencyKey);
    }

    @Recover
    public PointTransaction recoverFromOptimisticLockingFailure(OptimisticLockingFailureException ex,
                                                                UUID userId, TransactionType type, int points,
                                                                String source, String idempotencyKey) {
        return handleRecovery(ex, userId, type, points, source, idempotencyKey);
    }

    @Recover
    public PointTransaction recoverFromStaleObjectState(StaleObjectStateException ex,
                                                        UUID userId, TransactionType type, int points,
                                                        String source, String idempotencyKey) {
        return handleRecovery(ex, userId, type, points, source, idempotencyKey);
    }

    private PointTransaction handleRecovery(Exception ex, UUID userId, TransactionType type,
                                            int points, String source, String idempotencyKey) {
        log.error("💀 CRITICAL: Failed to record transaction after all retries for user {}", userId);
        log.error("💀 Transaction details - Type: {}, Points: {}, Source: {}, Key: {}",
                type, points, source, idempotencyKey);
        log.error("💀 Original error: {}", ex.getMessage());

        // In a production system, you might want to:
        // 1. Store this in a dead letter queue for manual processing
        // 2. Send an alert to operations team
        // 3. Store in a failed transactions table
        // 4. Implement compensation logic

        // For now, we'll throw a runtime exception to indicate failure
        throw new RuntimeException(
                String.format("Failed to process transaction for user %s after retries: %s",
                        userId, ex.getMessage()), ex);
    }

    /**
     * Convenience method for Kafka listeners and external services
     */
    public PointTransaction recordTransactionSafely(UUID userId, TransactionType type,
                                                    int points, String source, String idempotencyKey) {
        try {
            return recordTransactionWithIdempotency(userId, type, points, source, idempotencyKey);
        } catch (Exception e) {
            log.error("❌ Failed to record transaction safely for user {}: {}", userId, e.getMessage());
            // Instead of throwing, you could return null or a default transaction
            // depending on your business requirements
            throw e;
        }
    }

    public List<PointTransaction> getTransactionHistory(UUID userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    /**
     * Enhanced batch record transactions with individual error handling
     */
    @Transactional
    public void batchRecordTransactions(List<UUID> userIds, TransactionType type, int points, String source) {
        int successCount = 0;
        int failureCount = 0;

        for (UUID userId : userIds) {
            try {
                // Create unique idempotency key for batch operations
                String batchIdempotencyKey = String.format("batch-%s-%s-%d",
                        source, userId.toString(), System.currentTimeMillis());

                recordTransactionWithIdempotency(userId, type, points, source, batchIdempotencyKey);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("❌ Failed to record batch transaction for user {}: {}", userId, e.getMessage());
                // Continue processing other users even if one fails
            }
        }

        log.info("📊 Batch operation completed - Success: {}, Failures: {}, Total: {}",
                successCount, failureCount, userIds.size());
    }

    /**
     * Enhanced batch method with explicit idempotency keys
     */
    @Transactional
    public void batchRecordTransactionsWithKeys(List<UUID> userIds, TransactionType type,
                                                int points, String source, String batchId) {
        int successCount = 0;
        int failureCount = 0;

        for (UUID userId : userIds) {
            try {
                // Create deterministic idempotency key for reliable deduplication
                String idempotencyKey = String.format("batch-%s-%s-%s", batchId, source, userId.toString());

                recordTransactionWithIdempotency(userId, type, points, source, idempotencyKey);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("❌ Failed to record batch transaction for user {} in batch {}: {}",
                        userId, batchId, e.getMessage());
            }
        }

        log.info("📊 Batch {} completed - Success: {}, Failures: {}, Total: {}",
                batchId, successCount, failureCount, userIds.size());
    }

    /**
     * Search transactions by criteria
     */
    public List<PointTransaction> searchTransactions(UUID userId, TransactionType type,
                                                     LocalDateTime startDate, LocalDateTime endDate) {
        // TODO: Implement proper filtering based on type and date range
        // For now, return all transactions for the user
        if (type != null) {
            // You would need to add this method to your repository
            // return transactionRepository.findByUserIdAndTypeAndTransactionDateBetween(userId, type, startDate, endDate);
            return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        } else {
            return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        }
    }

    /**
     * Check if a transaction with the given idempotency key already exists
     */
    public boolean isTransactionProcessed(UUID userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return false;
        }
        return transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).isPresent();
    }

    /**
     * Get transaction by idempotency key
     */
    public Optional<PointTransaction> getTransactionByIdempotencyKey(UUID userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return Optional.empty();
        }
        return transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }
}