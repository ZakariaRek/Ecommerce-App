package com.Ecommerce.Product_Service.Payload.Review;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class ReviewStatsDTO {
    private UUID productId;
    private String productName;
    private Integer totalReviews;
    private Double averageRating;
    private Integer verifiedReviews;
    private Integer unverifiedReviews;
    private Map<String, Long> ratingDistribution; // e.g., {"1 star": 2, "2 stars": 5, ...}
    private LocalDateTime lastReviewDate;
    private LocalDateTime calculatedAt;

    public ReviewStatsDTO() {
        this.calculatedAt = LocalDateTime.now();
    }
}