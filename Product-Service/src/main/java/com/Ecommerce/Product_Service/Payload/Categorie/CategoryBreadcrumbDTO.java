package com.Ecommerce.Product_Service.Payload.Categorie;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBreadcrumbDTO {
    private UUID id;
    private String name;
    private Integer level;
}