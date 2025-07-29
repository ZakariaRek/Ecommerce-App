package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.Review;
import com.Ecommerce.Product_Service.Payload.Review.ReviewStatsDTO;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Repositories.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    // ====== READ OPERATIONS ======

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

    public List<Review> findVerifiedReviews() {
        return findAllReviews().stream()
                .filter(review -> review.getVerified() != null && review.getVerified())
                .collect(Collectors.toList());
    }

    public List<Review> findUnverifiedReviews() {
        return findAllReviews().stream()
                .filter(review -> review.getVerified() == null || !review.getVerified())
                .collect(Collectors.toList());
    }

    public List<Review> findRecentReviews(int limit) {
        return findAllReviews().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ====== WRITE OPERATIONS ======

    @Transactional
    public Review addReview(Review review) {
        // Validate review data
        validateReview(review);

        // Ensure review is new
        review.setId(null);
        review.setCreatedAt(LocalDateTime.now());
        review.setVerified(review.getVerified() != null ? review.getVerified() : false);

        // Fetch and validate product
        Optional<Product> productOpt = productRepository.findById(review.getProduct().getId());
        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with ID: " + review.getProduct().getId());
        }

        review.setProduct(productOpt.get());

        log.info("Creating review for product {} by user {}",
                review.getProduct().getId(), review.getUserId());

        return reviewRepository.save(review);
    }

    @Transactional
    public Optional<Review> updateReview(UUID id, Review updatedReview) {
        return reviewRepository.findById(id)
                .map(existingReview -> {
                    // Only update specific fields, preserve others
                    if (updatedReview.getRating() != null) {
                        validateRating(updatedReview.getRating());
                        existingReview.setRating(updatedReview.getRating());
                    }
                    if (updatedReview.getComment() != null) {
                        existingReview.setComment(updatedReview.getComment());
                    }
                    if (updatedReview.getVerified() != null) {
                        existingReview.setVerified(updatedReview.getVerified());
                    }

                    existingReview.setUpdatedAt(LocalDateTime.now());

                    log.info("Updated review with ID: {}", id);
                    return reviewRepository.save(existingReview);
                });
    }

    @Transactional
    public Optional<Review> verifyReview(UUID id) {
        return reviewRepository.findById(id)
                .map(review -> {
                    review.setVerified(true);
                    review.setUpdatedAt(LocalDateTime.now());

                    log.info("Verified review with ID: {}", id);
                    return reviewRepository.save(review);
                });
    }

    @Transactional
    public List<Review> bulkVerifyReviews(List<UUID> reviewIds) {
        List<Review> reviews = reviewRepository.findAllById(reviewIds);

        reviews.forEach(review -> {
            review.setVerified(true);
            review.setUpdatedAt(LocalDateTime.now());
        });

        List<Review> savedReviews = reviewRepository.saveAll(reviews);
        log.info("Bulk verified {} reviews", savedReviews.size());

        return savedReviews;
    }

    @Transactional
    public void deleteReview(UUID id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
            log.info("Deleted review with ID: {}", id);
        } else {
            throw new IllegalArgumentException("Review not found with ID: " + id);
        }
    }

    // ====== STATISTICS AND ANALYTICS ======

    public Double calculateAverageRatingForProduct(UUID productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }

        double sum = reviews.stream()
                .mapToInt(Review::getRating)
                .sum();

        return Math.round((sum / reviews.size()) * 100.0) / 100.0; // Round to 2 decimal places
    }

    public ReviewStatsDTO getProductReviewStatistics(UUID productId) {
        List<Review> reviews = findReviewsByProductId(productId);
        Optional<Product> productOpt = productRepository.findById(productId);

        ReviewStatsDTO stats = new ReviewStatsDTO();
        stats.setProductId(productId);

        if (productOpt.isPresent()) {
            stats.setProductName(productOpt.get().getName());
        }

        if (reviews.isEmpty()) {
            stats.setTotalReviews(0);
            stats.setAverageRating(0.0);
            stats.setVerifiedReviews(0);
            stats.setUnverifiedReviews(0);
            stats.setRatingDistribution(Map.of());
            return stats;
        }

        // Calculate basic statistics
        stats.setTotalReviews(reviews.size());
        stats.setAverageRating(calculateAverageRatingForProduct(productId));

        // Count verified/unverified reviews
        int verifiedCount = (int) reviews.stream()
                .filter(review -> review.getVerified() != null && review.getVerified())
                .count();
        stats.setVerifiedReviews(verifiedCount);
        stats.setUnverifiedReviews(reviews.size() - verifiedCount);

        // Rating distribution
        Map<String, Long> ratingDistribution = reviews.stream()
                .collect(Collectors.groupingBy(
                        review -> review.getRating() + (review.getRating() == 1 ? " star" : " stars"),
                        Collectors.counting()
                ));
        stats.setRatingDistribution(ratingDistribution);

        // Last review date
        reviews.stream()
                .max(Comparator.comparing(Review::getCreatedAt))
                .ifPresent(latestReview -> stats.setLastReviewDate(latestReview.getCreatedAt()));

        return stats;
    }

    public Map<String, Object> getOverallReviewStatistics() {
        List<Review> allReviews = findAllReviews();

        if (allReviews.isEmpty()) {
            return Map.of(
                    "totalReviews", 0,
                    "averageRating", 0.0,
                    "verifiedReviews", 0,
                    "productsWithReviews", 0
            );
        }

        double overallAverage = allReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        long verifiedCount = allReviews.stream()
                .filter(review -> review.getVerified() != null && review.getVerified())
                .count();

        long uniqueProducts = allReviews.stream()
                .filter(review -> review.getProduct() != null)
                .map(review -> review.getProduct().getId())
                .distinct()
                .count();

        return Map.of(
                "totalReviews", allReviews.size(),
                "averageRating", Math.round(overallAverage * 100.0) / 100.0,
                "verifiedReviews", verifiedCount,
                "productsWithReviews", uniqueProducts,
                "calculatedAt", LocalDateTime.now()
        );
    }

    // ====== VALIDATION METHODS ======

    private void validateReview(Review review) {
        if (review.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (review.getProduct() == null || review.getProduct().getId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }

        if (review.getRating() == null) {
            throw new IllegalArgumentException("Rating is required");
        }

        validateRating(review.getRating());

        if (review.getComment() != null && review.getComment().length() > 1000) {
            throw new IllegalArgumentException("Comment cannot exceed 1000 characters");
        }
    }

    private void validateRating(Integer rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }

    // ====== UTILITY METHODS ======

    public boolean hasUserReviewedProduct(UUID userId, UUID productId) {
        return findReviewsByProductId(productId).stream()
                .anyMatch(review -> userId.equals(review.getUserId()));
    }

    public List<Review> findReviewsByUserAndProduct(UUID userId, UUID productId) {
        return findReviewsByProductId(productId).stream()
                .filter(review -> userId.equals(review.getUserId()))
                .collect(Collectors.toList());
    }

    public List<Review> findReviewsByRatingRange(Integer minRating, Integer maxRating) {
        return findAllReviews().stream()
                .filter(review -> review.getRating() >= minRating && review.getRating() <= maxRating)
                .collect(Collectors.toList());
    }
}