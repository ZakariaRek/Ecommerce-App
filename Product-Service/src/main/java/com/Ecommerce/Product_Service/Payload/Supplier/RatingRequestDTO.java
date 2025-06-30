package com.Ecommerce.Product_Service.Payload.Supplier;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RatingRequestDTO {

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "0.0", message = "Rating cannot be negative")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    private BigDecimal rating;

    private String comment;
}