package com.Ecommerce.Order_Service.Repositories;

import com.Ecommerce.Order_Service.Entities.DiscountRule;
import com.Ecommerce.Order_Service.Entities.DiscountRuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, UUID> {
    List<DiscountRule> findByActiveTrue();
    List<DiscountRule> findByRuleTypeAndActiveTrue(DiscountRuleType ruleType);
    Optional<DiscountRule> findByRuleNameAndActiveTrue(String ruleName);
}