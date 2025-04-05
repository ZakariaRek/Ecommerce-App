package com.Ecommerce.Loyalty_Service.Repositories;

import java.util.Optional;
import java.util.UUID;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CRMRepository extends JpaRepository<CRM, UUID> {
    Optional<CRM> findByUserId(UUID userId);
}
