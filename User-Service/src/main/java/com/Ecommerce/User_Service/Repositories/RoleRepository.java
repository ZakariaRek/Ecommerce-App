package com.Ecommerce.User_Service.Repositories;


import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Models.ERole;
import org.springframework.stereotype.Repository;


@Repository
public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByName(ERole name);
}
