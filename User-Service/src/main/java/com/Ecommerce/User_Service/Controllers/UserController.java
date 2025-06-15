package com.Ecommerce.User_Service.Controllers;

import com.Ecommerce.User_Service.Models.ERole;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.UserStatus;
import com.Ecommerce.User_Service.Payload.Request.UpdateUserRequest;
import com.Ecommerce.User_Service.Payload.Response.MessageResponse;
import com.Ecommerce.User_Service.Repositories.RoleRepository;
import com.Ecommerce.User_Service.Services.RoleService;
import com.Ecommerce.User_Service.Services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

//    @PreAuthorize("ROLE_ADMIN")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PreAuthorize("ROLE_ADMIN")
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElse(new ResponseEntity(new MessageResponse("User not found"),HttpStatus.NOT_FOUND));
    }
    @PreAuthorize("ROLE_ADMIN")

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElse(new ResponseEntity(new MessageResponse("User not found"),HttpStatus.NOT_FOUND));
    }
    @PreAuthorize("ROLE_ADMIN")

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElse(new ResponseEntity(new MessageResponse("User not found"),HttpStatus.NOT_FOUND));
    }
    @PreAuthorize("ROLE_ADMIN")

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.getUserById(id)
                .map(existingUser -> {
                    existingUser.setUsername(request.getUsername());
                    existingUser.setEmail(request.getEmail());
                    existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
                    System.out.println(request.getRoles());
                    Set<Role> roles = new HashSet<>();
                    for (String roleStr : request.getRoles()) {
                        try {
                            ERole eRole = ERole.valueOf(roleStr); // Convert String to ERole
                            Role role = roleRepository.findByName(eRole)
                                    .orElseThrow(() -> new RuntimeException("Error: Role not found - " + roleStr));
                            roles.add(role);
                        } catch (IllegalArgumentException e) {
                            return ResponseEntity
                                    .badRequest()
                                    .body(new MessageResponse("Invalid role: " + roleStr));
                        }
                    }
                    existingUser.setRoles(roles);

                    User updatedUser = userService.updateUser(existingUser);
                    return new ResponseEntity<>(updatedUser, HttpStatus.OK);
                })
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("User not found")));
    }
    @PreAuthorize("ROLE_ADMIN")

    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<?> updateUserStatus(@PathVariable String id, @PathVariable String status) {
        try {
            return userService.getUserById(id)
                    .map(existingUser -> {
                        System.out.println(status);
                        System.out.println(existingUser);
                        System.out.println("existingUser");

                        existingUser.setStatus(UserStatus.valueOf(status));
                        System.out.println("existingUser");

                        User updatedUser = userService.updateUser(existingUser);
                        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
                    })
                    .orElse(new ResponseEntity(new MessageResponse("User not found"),HttpStatus.NOT_FOUND));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Invalid status value"));
        }
    }
    @PreAuthorize("ROLE_ADMIN")

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        return userService.getUserById(id)
                .map(user -> {
                    userService.deleteUser(id);
                    return ResponseEntity
                            .ok()
                            .body(new MessageResponse("User deleted successfully"));
                })
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("User not found")));
    }
}