package com.Ecommerce.Product_Service.Payload.Review;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewSummaryDTO {
    private UUID id;
    private UUID userId;
    private UUID productId;
    private String productName;
    private Integer rating;
    private Boolean verified;
    private String commentPreview; // First 100 characters of comment

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}