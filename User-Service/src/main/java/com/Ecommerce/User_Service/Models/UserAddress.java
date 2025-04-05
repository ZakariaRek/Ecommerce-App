package com.Ecommerce.User_Service.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.*;

import java.time.LocalDateTime;

@Data
@Document(collection = "user_addresses")
public class UserAddress {
    @Id
    private String id;

    @Indexed
    private String userId;

    private AddressType addressType = AddressType.HOME;

    private String street;

    private String city;

    private String state;

    private String country;

    private String zipCode;

    private boolean isDefault = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();
}
