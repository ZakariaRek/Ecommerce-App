package com.Ecommerce.User_Service.Repositories;


import com.Ecommerce.User_Service.Models.UserAddress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends MongoRepository<UserAddress, String> {
    List<UserAddress> findByUserId(String userId);
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(String userId);
    void deleteByUserId(String userId);
}