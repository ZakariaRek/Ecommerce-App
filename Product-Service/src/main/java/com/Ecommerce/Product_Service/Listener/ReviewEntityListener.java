package com.Ecommerce.Product_Service.Listener;


import com.Ecommerce.Product_Service.Entities.Review;
import com.Ecommerce.Product_Service.Services.Kakfa.ReviewEventService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewEntityListener {

    private static ReviewEventService reviewEventService;

    @Autowired
    public void setReviewEventService(ReviewEventService reviewEventService) {
        ReviewEntityListener.reviewEventService = reviewEventService;
    }

    @PostPersist
    public void postPersist(Review review) {
        if (reviewEventService != null) {
            reviewEventService.publishReviewCreatedEvent(review);

            // If it's a verified review at creation, also publish a verified event
            if (review.getVerified() != null && review.getVerified()) {
                reviewEventService.publishReviewVerifiedEvent(review);
            }

            // Also update product average rating

        }
    }

    @PostUpdate
    public void postUpdate(Review review) {
        if (reviewEventService != null) {
            // Basic update event
            reviewEventService.publishReviewUpdatedEvent(review);

            // The @PostUpdate doesn't provide the previous state
            // In a real application, you would need to use @PreUpdate to capture previous values
            // and store them in thread-local variables or a similar mechanism

            // Also update product average rating if the rating changed
        }
    }

    @PostRemove
    public void postRemove(Review review) {
        if (reviewEventService != null) {
            reviewEventService.publishReviewDeletedEvent(review);

            // Also update product average rating
        }
    }

    @PreUpdate
    public void preUpdate(Review review) {
        // In a real application, you would store the current state before the update
        // For example, using a ThreadLocal variable or similar mechanism
        // ThreadLocalContext.setPreviousRating(review.getRating());
        // ThreadLocalContext.setPreviousVerified(review.getVerified());
    }
}