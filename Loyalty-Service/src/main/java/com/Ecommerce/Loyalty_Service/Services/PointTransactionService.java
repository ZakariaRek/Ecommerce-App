package com.Ecommerce.Loyalty_Service.Services;

import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Repositories.PointTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PointTransactionService {
    @Autowired
    private PointTransactionRepository transactionRepository;

    @Autowired
    private CRMService crmService;

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

        return transactionRepository.save(transaction);
    }

    public List<PointTransaction> getTransactionHistory(UUID userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }
}
