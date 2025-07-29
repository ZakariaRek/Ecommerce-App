package com.Ecommerce.Product_Service.Payload.Review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewUpdateRequestDTO {

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    private Boolean verified;
}