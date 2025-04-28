package com.Ecommerce.Product_Service.Services.Kakfa;



import com.Ecommerce.Product_Service.Config.KafkaProducerConfig;
import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.Review;
import com.Ecommerce.Product_Service.Events.ReviewEvents;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import com.Ecommerce.Product_Service.Repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public void publishReviewCreatedEvent(Review review) {
        try {
            ReviewEvents.ReviewCreatedEvent event = ReviewEvents.ReviewCreatedEvent.builder()
                    .reviewId(review.getId())
                    .userId(review.getUserId())
                    .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                    .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .verified(review.getVerified())
                    .createdAt(LocalDateTime.now())
                    .build();

            log.info("Publishing review created event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_REVIEW_CREATED, review.getId().toString(), event);
            log.info("Review created event published successfully for review ID: {}", review.getId());
        } catch (Exception e) {
            log.error("Failed to publish review created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish review created event", e);
        }
    }

    public void publishReviewUpdatedEvent(Review review) {
        try {
            ReviewEvents.ReviewUpdatedEvent event = ReviewEvents.ReviewUpdatedEvent.builder()
                    .reviewId(review.getId())
                    .userId(review.getUserId())
                    .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                    .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                    .rating(review.getRating())
                    .comment(review.getComment())
                    .verified(review.getVerified())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing review updated event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_REVIEW_UPDATED, review.getId().toString(), event);
            log.info("Review updated event published successfully for review ID: {}", review.getId());
        } catch (Exception e) {
            log.error("Failed to publish review updated event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish review updated event", e);
        }
    }

    public void publishReviewDeletedEvent(Review review) {
        try {
            ReviewEvents.ReviewDeletedEvent event = ReviewEvents.ReviewDeletedEvent.builder()
                    .reviewId(review.getId())
                    .userId(review.getUserId())
                    .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                    .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                    .deletedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing review deleted event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_REVIEW_DELETED, review.getId().toString(), event);
            log.info("Review deleted event published successfully for review ID: {}", review.getId());
        } catch (Exception e) {
            log.error("Failed to publish review deleted event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish review deleted event", e);
        }
    }

    public void publishReviewVerifiedEvent(Review review) {
        try {
            ReviewEvents.ReviewVerifiedEvent event = ReviewEvents.ReviewVerifiedEvent.builder()
                    .reviewId(review.getId())
                    .userId(review.getUserId())
                    .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                    .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                    .rating(review.getRating())
                    .verifiedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing review verified event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_REVIEW_VERIFIED, review.getId().toString(), event);
            log.info("Review verified event published successfully for review ID: {}", review.getId());
        } catch (Exception e) {
            log.error("Failed to publish review verified event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish review verified event", e);
        }
    }

    public void publishReviewRatingChangedEvent(Review review, Integer previousRating) {
        try {
            ReviewEvents.ReviewRatingChangedEvent event = ReviewEvents.ReviewRatingChangedEvent.builder()
                    .reviewId(review.getId())
                    .userId(review.getUserId())
                    .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                    .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                    .previousRating(previousRating)
                    .newRating(review.getRating())
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("Publishing review rating changed event: {}", event);
            kafkaTemplate.send(KafkaProducerConfig.TOPIC_REVIEW_RATING_CHANGED, review.getId().toString(), event);
            log.info("Review rating changed event published successfully for review ID: {}", review.getId());
        } catch (Exception e) {
            log.error("Failed to publish review rating changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish review rating changed event", e);
        }
    }


}