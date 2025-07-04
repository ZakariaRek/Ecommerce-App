package com.Ecommerce.Product_Service.Payload.Review;


import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewResponseDtoFroPro {
    private UUID id;
    private UUID userId;
    private Integer rating;
    private String comment;
    private Boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}