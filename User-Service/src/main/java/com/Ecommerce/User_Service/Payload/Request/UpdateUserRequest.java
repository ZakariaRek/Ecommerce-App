package com.Ecommerce.User_Service.Payload.Request;

import com.Ecommerce.User_Service.Models.ERole;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Models.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.HashSet;
import java.util.Set;
@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 20)
    private String username;
    @Size(max = 50)
    @Email
    private String email;
    @Size(min = 6, max = 40)
    private String password;
    private UserStatus status ;
    @DBRef
    private Set<Role> roles ;




}