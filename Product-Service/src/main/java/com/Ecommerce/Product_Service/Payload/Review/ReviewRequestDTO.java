package com.Ecommerce.Product_Service.Payload.Review;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequestDTO {

    @NotNull(message = "User ID is required")
    @NotBlank(message = "User ID cannot be blank")
    private String userId; // Changed from UUID to String

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    private Boolean verified = false; // Default to false, admin can verify later
}