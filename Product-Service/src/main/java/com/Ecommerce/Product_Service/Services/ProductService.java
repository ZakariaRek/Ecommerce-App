package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> findProductById(UUID id) {
        return productRepository.findById(id);
    }

    public List<Product> findProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    @Transactional
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public boolean existsById(UUID id) {
        return productRepository.existsById(id);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public Optional<Product> updateProductStatus(UUID id, ProductStatus status) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setStatus(status);

                    // Handling inventory implications
                    if (status == ProductStatus.OUT_OF_STOCK) {
                        product.getInventory().setQuantity(0);
                        inventoryService.updateInventory(product.getInventory());
                    }

                    return productRepository.save(product);
                });
    }
}
