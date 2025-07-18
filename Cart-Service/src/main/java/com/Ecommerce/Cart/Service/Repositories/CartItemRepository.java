package com.Ecommerce.Cart.Service.Repositories;

import com.Ecommerce.Cart.Service.Models.CartItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing CartItem documents in MongoDB
 */
@Repository
public interface CartItemRepository extends MongoRepository<CartItem, UUID> {

    /**
     * Find all cart items belonging to a specific cart
     *
     * @param cartId the cart identifier
     * @return a list of cart items
     */
    List<CartItem> findByCartId(UUID cartId);

    /**
     * Find a specific cart item by cart ID and product ID
     *
     * @param cartId the cart identifier
     * @param productId the product identifier
     * @return the cart item if found
     */
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    /**
     * Find all cart items containing a specific product
     *
     * @param productId the product identifier
     * @return a list of cart items
     */
    List<CartItem> findByProductId(UUID productId);


}