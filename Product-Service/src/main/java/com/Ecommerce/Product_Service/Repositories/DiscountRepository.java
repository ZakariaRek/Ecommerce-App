package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Discount;
import com.Ecommerce.Product_Service.Entities.DiscountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID> {
    List<Discount> findByProductId(UUID productId);
    List<Discount> findByDiscountType(DiscountType discountType);
    List<Discount> findByStartDateBeforeAndEndDateAfter(LocalDateTime now, LocalDateTime now2);
}
