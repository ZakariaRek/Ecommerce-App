package com.Ecommerce.Order_Service.Repositories;

import com.Ecommerce.Order_Service.Entities.DiscountApplication;
import com.Ecommerce.Order_Service.Entities.DiscountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountApplicationRepository extends JpaRepository<DiscountApplication, UUID> {
    List<DiscountApplication> findByOrderIdOrderByAppliedAt(UUID orderId);
    List<DiscountApplication> findByDiscountType(DiscountType discountType);

    @Query("SELECT SUM(da.discountAmount) FROM DiscountApplication da WHERE da.order.userId = :userId AND da.appliedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalDiscountsByUserInPeriod(@Param("userId") UUID userId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
}