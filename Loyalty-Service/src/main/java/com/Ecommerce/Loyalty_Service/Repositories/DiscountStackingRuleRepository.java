package com.Ecommerce.Loyalty_Service.Repositories;

import com.Ecommerce.Loyalty_Service.Entities.DiscountStackingRule;
import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountStackingRuleRepository extends JpaRepository<DiscountStackingRule, UUID> {
    List<DiscountStackingRule> findByActiveTrueOrderByPriorityOrder();
    Optional<DiscountStackingRule> findByPrimaryDiscountTypeAndActiveTrue(DiscountType primaryDiscountType);
}