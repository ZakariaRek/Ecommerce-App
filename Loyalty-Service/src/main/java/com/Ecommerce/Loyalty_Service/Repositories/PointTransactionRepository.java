package com.Ecommerce.Loyalty_Service.Repositories;

import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    List<PointTransaction> findByUserIdOrderByTransactionDateDesc(UUID userId);


    // New methods for idempotency support
    Optional<PointTransaction> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);



}