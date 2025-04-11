package com.Ecommerce.Cart.Service.Repositories;

import com.Ecommerce.Cart.Service.Models.ShoppingCart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingCartRepository extends MongoRepository<ShoppingCart, UUID> {
    Optional<ShoppingCart> findByUserId(UUID userId);
    List<ShoppingCart> findByExpiresAtBefore(LocalDateTime dateTime);
}