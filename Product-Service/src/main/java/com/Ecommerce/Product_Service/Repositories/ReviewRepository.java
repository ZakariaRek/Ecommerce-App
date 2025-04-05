package com.Ecommerce.Product_Service.Repositories;

import com.Ecommerce.Product_Service.Entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByProductId(UUID productId);
    List<Review> findByUserId(UUID userId);
    List<Review> findByRatingGreaterThanEqual(Integer rating);
}

