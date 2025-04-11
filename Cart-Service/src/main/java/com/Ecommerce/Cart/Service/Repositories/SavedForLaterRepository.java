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
}