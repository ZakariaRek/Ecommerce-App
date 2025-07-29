package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Entities.Review;
import com.Ecommerce.Product_Service.Payload.Review.*;
import com.Ecommerce.Product_Service.Services.ReviewService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reviews")
@Validated
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewMapper reviewMapper;

    @GetMapping
    public ResponseEntity<List<ReviewSummaryDTO>> getAllReviews() {
        try {
            List<Review> reviews = reviewService.findAllReviews();
            List<ReviewSummaryDTO> reviewDTOs = reviewMapper.toSummaryDTOList(reviews);
            return ResponseEntity.ok(reviewDTOs);
        } catch (Exception e) {
            log.error("Error retrieving all reviews", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving reviews");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> getReviewById(@PathVariable UUID id) {
        try {
            return reviewService.findReviewById(id)
                    .map(reviewMapper::toResponseDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving review with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving review");
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewSummaryDTO>> getReviewsByProductId(@PathVariable UUID productId) {
        try {
            List<Review> reviews = reviewService.findReviewsByProductId(productId);
            List<ReviewSummaryDTO> reviewDTOs = reviewMapper.toSummaryDTOList(reviews);
            return ResponseEntity.ok(reviewDTOs);
        } catch (Exception e) {
            log.error("Error retrieving reviews for product ID: {}", productId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving product reviews");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewSummaryDTO>> getReviewsByUserId(@PathVariable UUID userId) {
        try {
            List<Review> reviews = reviewService.findReviewsByUserId(userId);
            List<ReviewSummaryDTO> reviewDTOs = reviewMapper.toSummaryDTOList(reviews);
            return ResponseEntity.ok(reviewDTOs);
        } catch (Exception e) {
            log.error("Error retrieving reviews for user ID: {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving user reviews");
        }
    }

    @GetMapping("/rating/{minimumRating}")
    public ResponseEntity<List<ReviewSummaryDTO>> getReviewsByMinimumRating(@PathVariable Integer minimumRating) {
        try {
            if (minimumRating < 1 || minimumRating > 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
            }

            List<Review> reviews = reviewService.findReviewsByMinimumRating(minimumRating);
            List<ReviewSummaryDTO> reviewDTOs = reviewMapper.toSummaryDTOList(reviews);
            return ResponseEntity.ok(reviewDTOs);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving reviews with minimum rating: {}", minimumRating, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving reviews by rating");
        }
    }

    @GetMapping("/verified")
    public ResponseEntity<List<ReviewSummaryDTO>> getVerifiedReviews() {
        try {
            List<Review> allReviews = reviewService.findAllReviews();
            List<ReviewSummaryDTO> verifiedReviews = allReviews.stream()
                    .filter(review -> review.getVerified() != null && review.getVerified())
                    .map(reviewMapper::toSummaryDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(verifiedReviews);
        } catch (Exception e) {
            log.error("Error retrieving verified reviews", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving verified reviews");
        }
    }

    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(@Valid @RequestBody ReviewRequestDTO reviewRequest) {
        try {
            Review review = reviewMapper.toEntity(reviewRequest);
            Review createdReview = reviewService.addReview(review);
            ReviewResponseDTO responseDTO = reviewMapper.toResponseDTO(createdReview);

            log.info("Review created successfully with ID: {}", createdReview.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid review data: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error creating review", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating review");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequestDTO reviewRequest) {
        try {
            return reviewService.findReviewById(id)
                    .map(existingReview -> {
                        reviewMapper.updateEntityFromDTO(existingReview, reviewRequest);
                        return reviewService.updateReview(id, existingReview)
                                .map(reviewMapper::toResponseDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid review update data for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error updating review with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating review");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> partialUpdateReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewUpdateRequestDTO updateRequest) {
        try {
            return reviewService.findReviewById(id)
                    .map(existingReview -> {
                        reviewMapper.updateEntityFromUpdateDTO(existingReview, updateRequest);
                        return reviewService.updateReview(id, existingReview)
                                .map(reviewMapper::toResponseDTO)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid review partial update data for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error partially updating review with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating review");
        }
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ReviewResponseDTO> verifyReview(@PathVariable UUID id) {
        try {
            return reviewService.verifyReview(id)
                    .map(reviewMapper::toResponseDTO)
                    .map(verifiedReview -> {
                        log.info("Review verified successfully with ID: {}", id);
                        return ResponseEntity.ok(verifiedReview);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error verifying review with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error verifying review");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        try {
            if (reviewService.findReviewById(id).isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            reviewService.deleteReview(id);
            log.info("Review deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting review with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting review");
        }
    }

    @GetMapping("/product/{productId}/average-rating")
    public ResponseEntity<Map<String, Double>> getAverageRatingForProduct(@PathVariable UUID productId) {
        try {
            Double averageRating = reviewService.calculateAverageRatingForProduct(productId);
            return ResponseEntity.ok(Map.of("averageRating", averageRating));
        } catch (Exception e) {
            log.error("Error calculating average rating for product ID: {}", productId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error calculating average rating");
        }
    }

    @GetMapping("/product/{productId}/statistics")
    public ResponseEntity<ReviewStatsDTO> getProductReviewStatistics(@PathVariable UUID productId) {
        try {
            ReviewStatsDTO statistics = reviewService.getProductReviewStatistics(productId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error calculating statistics for product ID: {}", productId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error calculating review statistics");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ReviewSummaryDTO>> searchReviews(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(required = false) Boolean verified) {
        try {
            List<Review> allReviews = reviewService.findAllReviews();

            List<ReviewSummaryDTO> filteredReviews = allReviews.stream()
                    .filter(review -> {
                        boolean matches = true;

                        if (userId != null) {
                            matches = matches && userId.equals(review.getUserId());
                        }

                        if (productId != null) {
                            matches = matches && review.getProduct() != null &&
                                    productId.equals(review.getProduct().getId());
                        }

                        if (minRating != null) {
                            matches = matches && review.getRating() != null &&
                                    review.getRating() >= minRating;
                        }

                        if (maxRating != null) {
                            matches = matches && review.getRating() != null &&
                                    review.getRating() <= maxRating;
                        }

                        if (verified != null) {
                            matches = matches && verified.equals(review.getVerified());
                        }

                        return matches;
                    })
                    .map(reviewMapper::toSummaryDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(filteredReviews);
        } catch (Exception e) {
            log.error("Error searching reviews", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching reviews");
        }
    }

    @PostMapping("/bulk-verify")
    public ResponseEntity<List<ReviewResponseDTO>> bulkVerifyReviews(@RequestBody List<UUID> reviewIds) {
        try {
            if (reviewIds == null || reviewIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review IDs list cannot be empty");
            }

            List<Review> verifiedReviews = reviewService.bulkVerifyReviews(reviewIds);
            List<ReviewResponseDTO> responseList = reviewMapper.toResponseDTOList(verifiedReviews);

            log.info("Bulk verified {} reviews", verifiedReviews.size());
            return ResponseEntity.ok(responseList);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error bulk verifying reviews", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error bulk verifying reviews");
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOverallReviewStatistics() {
        try {
            Map<String, Object> statistics = reviewService.getOverallReviewStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error retrieving overall review statistics", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving review statistics");
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ReviewSummaryDTO>> getRecentReviews(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit cannot exceed 100");
            }

            List<Review> recentReviews = reviewService.findRecentReviews(limit);
            List<ReviewSummaryDTO> responseList = reviewMapper.toSummaryDTOList(recentReviews);
            return ResponseEntity.ok(responseList);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving recent reviews", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving recent reviews");
        }
    }

    @GetMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getUserReviewsForProduct(
            @PathVariable UUID userId,
            @PathVariable UUID productId) {
        try {
            List<Review> reviews = reviewService.findReviewsByUserAndProduct(userId, productId);
            List<ReviewResponseDTO> responseList = reviewMapper.toResponseDTOList(reviews);
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            log.error("Error retrieving reviews for user {} and product {}", userId, productId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving user reviews for product");
        }
    }
}