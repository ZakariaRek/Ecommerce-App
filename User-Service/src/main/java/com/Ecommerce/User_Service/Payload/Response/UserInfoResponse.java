package com.Ecommerce.User_Service.Payload.Response;

import lombok.Data;

import java.util.List;
import java.util.Set;
@Data
public class UserInfoResponse {
    private String id;
    private String username;
    private String email;
    private List<String> roles;

    public UserInfoResponse(String id, String username, String email, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

}
