package com.Ecommerce.Product_Service.Payload.Review;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.Review;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ReviewMapper {

    /**
     * Convert ReviewRequestDTO to Review entity
     */
    public Review toEntity(ReviewRequestDTO dto) {
        if (dto == null) return null;

        Review review = new Review();
        review.setUserId(parseUUID(dto.getUserId()));
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setVerified(dto.getVerified() != null ? dto.getVerified() : false);

        // Set product - will be resolved in service layer
        if (dto.getProductId() != null) {
            Product product = new Product();
            product.setId(dto.getProductId());
            review.setProduct(product);
        }

        return review;
    }

    /**
     * Utility method to parse UUID from string, handling both dashed and non-dashed formats
     */
    private UUID parseUUID(String uuidString) {
        if (uuidString == null || uuidString.trim().isEmpty()) {
            throw new IllegalArgumentException("UUID string cannot be null or empty");
        }

        // Remove any whitespace
        uuidString = uuidString.trim();

        // Check if it's already a properly formatted UUID with dashes
        if (uuidString.contains("-") && uuidString.length() == 36) {
            try {
                return UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
            }
        }

        // Handle different lengths of hex strings
        if (!uuidString.contains("-")) {
            if (uuidString.length() == 32) {
                // Standard 32-character hex string
                uuidString = formatUUIDString(uuidString);
            } else if (uuidString.length() == 24) {
                // 24-character hex string - pad with zeros to make it 32
                uuidString = uuidString + "00000000"; // Add 8 zeros
                uuidString = formatUUIDString(uuidString);
            } else if (uuidString.length() < 32) {
                // Pad with leading zeros to make it 32 characters
                uuidString = String.format("%32s", uuidString).replace(' ', '0');
                uuidString = formatUUIDString(uuidString);
            } else {
                throw new IllegalArgumentException("UUID string too long: " + uuidString.length() + " characters");
            }
        }

        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
        }
    }

    /**
     * Format a 32-character hex string into UUID format with dashes
     */
    private String formatUUIDString(String hexString) {
        if (hexString.length() != 32) {
            throw new IllegalArgumentException("Hex string must be exactly 32 characters");
        }

        return hexString.substring(0, 8) + "-" +
                hexString.substring(8, 12) + "-" +
                hexString.substring(12, 16) + "-" +
                hexString.substring(16, 20) + "-" +
                hexString.substring(20, 32);
    }

    /**
     * Convert Review entity to ReviewResponseDTO
     */
    public ReviewResponseDTO toResponseDTO(Review entity) {
        if (entity == null) return null;

        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setVerified(entity.getVerified());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Map product information if available
        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getId());
            dto.setProductName(entity.getProduct().getName());
            dto.setProductSku(entity.getProduct().getSku());
        }

        return dto;
    }

    /**
     * Convert Review entity to ReviewSummaryDTO
     */
    public ReviewSummaryDTO toSummaryDTO(Review entity) {
        if (entity == null) return null;

        ReviewSummaryDTO dto = new ReviewSummaryDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setRating(entity.getRating());
        dto.setVerified(entity.getVerified());
        dto.setCreatedAt(entity.getCreatedAt());

        // Create comment preview (first 100 characters)
        if (entity.getComment() != null) {
            String comment = entity.getComment();
            dto.setCommentPreview(comment.length() > 100 ?
                    comment.substring(0, 100) + "..." : comment);
        }

        // Map product information if available
        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getId());
            dto.setProductName(entity.getProduct().getName());
        }

        return dto;
    }

    /**
     * Update existing Review entity from ReviewRequestDTO
     */
    public void updateEntityFromDTO(Review entity, ReviewRequestDTO dto) {
        if (entity == null || dto == null) return;

        if (dto.getRating() != null) {
            entity.setRating(dto.getRating());
        }
        if (dto.getComment() != null) {
            entity.setComment(dto.getComment());
        }
        if (dto.getVerified() != null) {
            entity.setVerified(dto.getVerified());
        }
    }

    /**
     * Update existing Review entity from ReviewUpdateRequestDTO
     */
    public void updateEntityFromUpdateDTO(Review entity, ReviewUpdateRequestDTO dto) {
        if (entity == null || dto == null) return;

        if (dto.getRating() != null) {
            entity.setRating(dto.getRating());
        }
        if (dto.getComment() != null) {
            entity.setComment(dto.getComment());
        }
        if (dto.getVerified() != null) {
            entity.setVerified(dto.getVerified());
        }
    }

    /**
     * Convert list of Review entities to list of ReviewResponseDTOs
     */
    public List<ReviewResponseDTO> toResponseDTOList(List<Review> entities) {
        if (entities == null) return null;

        return entities.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Review entities to list of ReviewSummaryDTOs
     */
    public List<ReviewSummaryDTO> toSummaryDTOList(List<Review> entities) {
        if (entities == null) return null;

        return entities.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }
}