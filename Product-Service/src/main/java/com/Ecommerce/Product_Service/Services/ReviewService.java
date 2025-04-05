package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.Review;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Repositories.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Review> findAllReviews() {
        return reviewRepository.findAll();
    }

    public Optional<Review> findReviewById(UUID id) {
        return reviewRepository.findById(id);
    }

    public List<Review> findReviewsByProductId(UUID productId) {
        return reviewRepository.findByProductId(productId);
    }

    public List<Review> findReviewsByUserId(UUID userId) {
        return reviewRepository.findByUserId(userId);
    }

    public List<Review> findReviewsByMinimumRating(Integer rating) {
        return reviewRepository.findByRatingGreaterThanEqual(rating);
    }

    @Transactional
    public Review addReview(Review review) {
        // Ensure review is new
        review.setId(null);
        review.setCreatedAt(LocalDateTime.now());
        review.setVerified(false);

        // Check if product exists
        Optional<Product> product = productRepository.findById(review.getProduct().getId());
        if (product.isEmpty()) {
            throw new IllegalArgumentException("Product not found");
        }

        review.setProduct(product.get());
        return reviewRepository.save(review);
    }

    @Transactional
    public Optional<Review> updateReview(UUID id, Review updatedReview) {
        return reviewRepository.findById(id)
                .map(existingReview -> {
                    // Only update specific fields, preserve others
                    if (updatedReview.getRating() != null) {
                        existingReview.setRating(updatedReview.getRating());
                    }
                    if (updatedReview.getComment() != null) {
                        existingReview.setComment(updatedReview.getComment());
                    }

                    existingReview.setUpdatedAt(LocalDateTime.now());
                    return reviewRepository.save(existingReview);
                });
    }

    @Transactional
    public Optional<Review> verifyReview(UUID id) {
        return reviewRepository.findById(id)
                .map(review -> {
                    review.setVerified(true);
                    review.setUpdatedAt(LocalDateTime.now());
                    return reviewRepository.save(review);
                });
    }

    @Transactional
    public void deleteReview(UUID id) {
        reviewRepository.deleteById(id);
    }

    public Double calculateAverageRatingForProduct(UUID productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }

        double sum = reviews.stream()
                .mapToInt(Review::getRating)
                .sum();

        return sum / reviews.size();
    }
}