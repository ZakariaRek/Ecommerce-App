package com.Ecommerce.Product_Service.Services;

import com.Ecommerce.Product_Service.Entities.Product;
import com.Ecommerce.Product_Service.Entities.ProductStatus;
import com.Ecommerce.Product_Service.Repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.Ecommerce.Product_Service.Services.Kakfa.ProductEventService;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductEventService productEventService;

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

        productEventService.publishProductCreatedEvent(product);

        return productRepository.save(product);
    }

    public boolean existsById(UUID id) {
        return productRepository.existsById(id);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productEventService.publishProductDeletedEvent(id);

        productRepository.deleteById(id);
    }

    @Transactional
    public Optional<Product> updateProductStatus(UUID id, ProductStatus status) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setStatus(status);
                    productEventService.publishStatusChangedEvent(product, status);
                    // Handling inventory implications
                    if (status == ProductStatus.OUT_OF_STOCK) {
                        product.getInventory().setQuantity(0);
                        inventoryService.updateInventory(product.getInventory());
                    }

                    return productRepository.save(product);
                });
    }

    /**
     * Updates a product with the provided data.
     * This method supports partial updates (PATCH).
     *
     * @param id the ID of the product to update
     * @param productData the product data containing fields to update
     * @return an Optional containing the updated product if found, or empty if not found
     */
    @Transactional
    public Optional<Product> updateProduct(UUID id, Product productData) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    // Track original values for event publishing
                    ProductStatus originalStatus = existingProduct.getStatus();
                    BigDecimal originalPrice = existingProduct.getPrice();
                    Integer originalStock = existingProduct.getStock();

                    // Update only non-null fields from the productData
                    if (productData.getName() != null) {
                        existingProduct.setName(productData.getName());
                    }
                    if (productData.getDescription() != null) {
                        existingProduct.setDescription(productData.getDescription());
                    }
                    if (productData.getPrice() != null) {
                        existingProduct.setPrice(productData.getPrice());
                    }
                    if (productData.getStock() != null) {
                        existingProduct.setStock(productData.getStock());

                        // Update inventory if stock changed
                        if (existingProduct.getInventory() != null) {
                            existingProduct.getInventory().setQuantity(productData.getStock());
                            inventoryService.updateInventory(existingProduct.getInventory());
                        }
                    }
                    if (productData.getSku() != null) {
                        existingProduct.setSku(productData.getSku());
                    }
                    if (productData.getWeight() != null) {
                        existingProduct.setWeight(productData.getWeight());
                    }
                    if (productData.getDimensions() != null) {
                        existingProduct.setDimensions(productData.getDimensions());
                    }
                    if (productData.getStatus() != null) {
                        existingProduct.setStatus(productData.getStatus());
                    }
                    if (productData.getImages() != null && !productData.getImages().isEmpty()) {
                        existingProduct.setImages(productData.getImages());
                    }

                    // Publish relevant events
                    if (productData.getStatus() != null && !productData.getStatus().equals(originalStatus)) {
                        productEventService.publishStatusChangedEvent(existingProduct, productData.getStatus());
                    }
                    if (productData.getPrice() != null && !productData.getPrice().equals(originalPrice)) {
                        productEventService.publishPriceChangedEvent(existingProduct, productData.getPrice());
                    }
                    if (productData.getStock() != null && !productData.getStock().equals(originalStock)) {
                        productEventService.publishStockChangedEvent(existingProduct, productData.getStock());
                    }
                    productEventService.publishProductUpdatedEvent(existingProduct);
                    return productRepository.save(existingProduct);
                });
    }
}