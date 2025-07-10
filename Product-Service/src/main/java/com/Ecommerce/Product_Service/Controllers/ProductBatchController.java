package com.Ecommerce.Product_Service.Controllers;

import com.Ecommerce.Product_Service.Payload.Product.ProductBatchRequestDTO;
import com.Ecommerce.Product_Service.Payload.Product.ProductBatchResponseDTO;
import com.Ecommerce.Product_Service.Services.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/batch")
@Slf4j
public class ProductBatchController {

    @Autowired
    private ProductService productService;

    @PostMapping("/product-info")
    public ResponseEntity<List<ProductBatchResponseDTO>> getBatchProductInfo(
            @RequestBody ProductBatchRequestDTO request) {

        log.info("Fetching batch product info for {} products", request.getProductIds().size());

        List<ProductBatchResponseDTO> productInfos = productService.getBatchProductInfo(request.getProductIds());

        return ResponseEntity.ok(productInfos);
    }
}