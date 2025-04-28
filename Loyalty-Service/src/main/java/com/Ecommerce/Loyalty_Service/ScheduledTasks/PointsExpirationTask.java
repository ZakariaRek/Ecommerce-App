package com.Ecommerce.Loyalty_Service.ScheduledTasks;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Listeners.CRMMongoListener;
import com.Ecommerce.Loyalty_Service.Repositories.CRMRepository;
import com.Ecommerce.Loyalty_Service.Services.Kafka.CRMKafkaService;
import com.Ecommerce.Loyalty_Service.Services.PointTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to handle points expiration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointsExpirationTask {

    private final CRMRepository crmRepository;
    private final PointTransactionService transactionService;
    private final CRMKafkaService kafkaService;
    private final CRMMongoListener crmMongoListener;

    /**
     * Run daily at 3:00 AM to check for points expiration
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void processPointsExpiration() {
        log.info("Starting scheduled points expiration task");

        // In a real implementation, you would look for transactions that are about to expire
        // For this example, we'll expire points for users who haven't been active for 12 months
        LocalDateTime inactivityCutoff = LocalDateTime.now().minusMonths(12);

        // Find users who haven't been active
        // This would need a custom repository method
        // List<CRM> inactiveUsers = crmRepository.findByLastActivityBefore(inactivityCutoff);

        // For this example, we'll just use a placeholder
        List<CRM> inactiveUsers = List.of(); // Empty list as placeholder

        log.info("Found {} inactive users with points to expire", inactiveUsers.size());

        for (CRM crm : inactiveUsers) {
            try {
                if (crm.getTotalPoints() > 0) {
                    // Store previous state for Mongo listener
                    crmMongoListener.storeStateBeforeSave(crm);

                    // Calculate points to expire - in this case, all points
                    int pointsToExpire = crm.getTotalPoints();

                    // Record the expiration transaction
                    transactionService.recordTransaction(
                            crm.getUserId(),
                            TransactionType.EXPIRE,
                            pointsToExpire,
                            "Points Expiration - Inactivity"
                    );

                    // Send direct Kafka event
                    kafkaService.publishPointsExpired(crm, pointsToExpire);

                    log.info("Expired {} points for inactive user {}",
                            pointsToExpire, crm.getUserId());
                }
            } catch (Exception e) {
                log.error("Error processing points expiration for user {}: {}",
                        crm.getUserId(), e.getMessage());
                // Continue with next user
            }
        }

        log.info("Completed scheduled points expiration task");
    }
}