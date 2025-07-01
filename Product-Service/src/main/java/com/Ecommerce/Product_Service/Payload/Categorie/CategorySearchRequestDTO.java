package com.Ecommerce.Product_Service.Payload.Categorie;

import lombok.Data;

import java.util.UUID;


@Data
public class CategorySearchRequestDTO {
    private String name;
    private UUID parentId;
    private Integer level;
    private Boolean hasProducts;
}