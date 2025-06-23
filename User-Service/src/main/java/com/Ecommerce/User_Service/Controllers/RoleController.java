package com.Ecommerce.User_Service.Controllers;

import com.Ecommerce.User_Service.Models.ERole;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Payload.Response.MessageResponse;
import com.Ecommerce.User_Service.Services.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
//@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable String id) {
        return roleService.getRoleById(id)
                .map(role -> new ResponseEntity<>(role, HttpStatus.OK))
                .orElse(new ResponseEntity(new MessageResponse("Role not found"), HttpStatus.NOT_FOUND));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<?> getRoleByName(@PathVariable String name) {
        try {
            ERole roleName = ERole.valueOf(name);
            return roleService.getRoleByName(roleName)
                    .map(role -> new ResponseEntity<>(role, HttpStatus.OK))
                    .orElse(new ResponseEntity(new MessageResponse("Role not found"), HttpStatus.NOT_FOUND));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Invalid role name"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createRole(@Valid @RequestBody Role role) {
        Role savedRole = roleService.createRole(role);
        return new ResponseEntity<>(savedRole, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable String id, @Valid @RequestBody Role role) {
        return roleService.getRoleById(id)
                .map(existingRole -> {
                    role.setId(id);
                    Role updatedRole = roleService.updateRole(role);
                    return new ResponseEntity<>(updatedRole, HttpStatus.OK);
                }).orElse(new ResponseEntity(new MessageResponse("Role not found"), HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable String id) {
        return roleService.getRoleById(id)
                .map(role -> {
                    roleService.deleteRole(id);
                    return ResponseEntity
                            .ok()
                            .body(new MessageResponse("Role deleted successfully"));
                })
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("Role not found")));
    }

    @PostMapping("/init")
    public ResponseEntity<?> initializeRoles() {
        roleService.initRoles();
        return ResponseEntity
                .ok()
                .body(new MessageResponse("Roles initialized successfully"));
    }
}