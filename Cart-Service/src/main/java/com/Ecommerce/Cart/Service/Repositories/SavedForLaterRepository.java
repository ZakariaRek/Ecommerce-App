package com.Ecommerce.Cart.Service.Repositories;


import com.Ecommerce.Cart.Service.Models.SavedForLater;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SavedForLaterRepository extends MongoRepository<SavedForLater, UUID> {
    List<SavedForLater> findByUserId(UUID userId);
    void deleteByUserIdAndProductId(UUID userId, UUID productId);

//    List<SavedForLater> findByUserId(UUID userId);

    /**
     * Delete a specific saved item by user and product
     */
//    void deleteByUserIdAndProductId(UUID userId, UUID productId);

    /**
     * Check if a product is saved by a user
     */
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    /**
     * Count saved items for a user
     */
    long countByUserId(UUID userId);

    /**
     * Delete all saved items for a user
     */
    void deleteByUserId(UUID userId);
}