package com.Ecommerce.Order_Service.Repositories;

import com.Ecommerce.Order_Service.Entities.DiscountApplication;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.UUID;

@Repository
public interface DiscountApplicationRepository extends JpaRepository<DiscountApplication, UUID> {

}