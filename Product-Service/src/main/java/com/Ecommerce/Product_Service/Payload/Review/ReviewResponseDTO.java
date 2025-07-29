package com.Ecommerce.Product_Service.Payload.Review;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewResponseDTO {
    private UUID id;
    private UUID userId;
    private UUID productId;
    private String productName;
    private String productSku;
    private Integer rating;
    private String comment;
    private Boolean verified;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}