package com.Ecommerce.Loyalty_Service.Repositories;

import com.Ecommerce.Loyalty_Service.Entities.CouponUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CouponUsageHistoryRepository extends JpaRepository<CouponUsageHistory, UUID> {
    List<CouponUsageHistory> findByUserIdOrderByUsedAtDesc(UUID userId);
    List<CouponUsageHistory> findByCouponIdOrderByUsedAtDesc(UUID couponId);
    Long countByUserIdAndCouponId(UUID userId, UUID couponId);

    @Query("SELECT SUM(cuh.discountAmount) FROM CouponUsageHistory cuh WHERE cuh.userId = :userId AND cuh.usedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCouponDiscountsByUserInPeriod(@Param("userId") UUID userId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
}
