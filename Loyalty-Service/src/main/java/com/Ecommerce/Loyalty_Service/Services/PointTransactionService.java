package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Repositories.PointTransactionRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.TransactionKafkaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointTransactionService {
    private final PointTransactionRepository transactionRepository;
    private final CRMService crmService;
    private final TransactionKafkaService kafkaService;

    @Transactional
    public PointTransaction recordTransaction(UUID userId, TransactionType type, int points, String source) {
        PointTransaction transaction = new PointTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setPoints(points);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setSource(source);

        // Update the CRM based on transaction type
        if (type == TransactionType.EARN) {
            crmService.earnPoints(userId, points, source);
        } else if (type == TransactionType.REDEEM) {
            crmService.redeemPoints(userId, points);
        }

        // Get current balance after CRM update
        int currentBalance = crmService.getByUserId(userId).getTotalPoints();
        transaction.setBalance(currentBalance);

        // Save the transaction (MongoDB listener will trigger event)
        PointTransaction savedTransaction = transactionRepository.save(transaction);

        // Direct Kafka event (in case MongoDB listener doesn't catch it or we need additional context)
        kafkaService.publishTransactionRecorded(savedTransaction);

        log.info("Recorded {} transaction of {} points for user {}, new balance: {}",
                type, points, userId, currentBalance);

        return savedTransaction;
    }

    public List<PointTransaction> getTransactionHistory(UUID userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    /**
     * Batch record transactions for multiple users (e.g., for special events or promotions)
     */
    @Transactional
    public void batchRecordTransactions(List<UUID> userIds, TransactionType type, int points, String source) {
        for (UUID userId : userIds) {
            try {
                recordTransaction(userId, type, points, source);
            } catch (Exception e) {
                log.error("Failed to record transaction for user {}: {}", userId, e.getMessage());
                // Continue processing other users even if one fails
            }
        }

        log.info("Batch recorded {} transactions of {} points for {} users",
                type, points, userIds.size());
    }

    /**
     * Search transactions by criteria
     */
    public List<PointTransaction> searchTransactions(UUID userId, TransactionType type, LocalDateTime startDate, LocalDateTime endDate) {
        // In a real implementation, you would create a custom repository method or use a query builder
        // For this example, we'll just handle the simple case of searching by user ID and type
        if (type != null) {
            // You would need to add this method to your repository
            // return transactionRepository.findByUserIdAndTypeAndTransactionDateBetween(userId, type, startDate, endDate);

            // For now, just return all transactions for the user
            return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        } else {
            return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        }
    }
}