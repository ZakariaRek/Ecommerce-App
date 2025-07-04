package com.Ecommerce.Product_Service.Payload.Product;

import com.Ecommerce.Product_Service.Entities.Category;
import com.Ecommerce.Product_Service.Entities.Discount;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Entities.Review;
import com.Ecommerce.Product_Service.Payload.Categorie.CategoryResponseDtoForPro;
import com.Ecommerce.Product_Service.Payload.Discont.DiscountResponseDtoForPro;
import com.Ecommerce.Product_Service.Payload.Review.ReviewResponseDtoFroPro;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
public class ProductResponseAllDto {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String sku;
    private BigDecimal weight;
    private String dimensions;
    private List<String> images;
    private List<CategoryResponseDtoForPro> categories;
    private List<DiscountResponseDtoForPro> discounts;
    private List<ReviewResponseDtoFroPro> reviews;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
