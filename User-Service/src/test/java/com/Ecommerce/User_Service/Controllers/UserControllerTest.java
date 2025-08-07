package com.Ecommerce.User_Service.Controllers;

import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Models.ERole;
import com.Ecommerce.User_Service.Models.UserStatus;
import com.Ecommerce.User_Service.Payload.Request.UpdateUserRequest;
import com.Ecommerce.User_Service.Repositories.RoleRepository;
import com.Ecommerce.User_Service.Services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    private User user1;
    private User user2;
    private Role userRole;
    private Role adminRole;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Setup roles
        userRole = new Role();
        userRole.setId("role1");
        userRole.setName(ERole.ROLE_USER);

        adminRole = new Role();
        adminRole.setId("role2");
        adminRole.setName(ERole.ROLE_ADMIN);

        // Setup users
        user1 = new User();
        user1.setId("user1");
        user1.setUsername("user1");
        user1.setEmail("user1@test.com");
        user1.setStatus(UserStatus.ACTIVE);
        Set<Role> user1Roles = new HashSet<>();
        user1Roles.add(userRole);
        user1.setRoles(user1Roles);

        user2 = new User();
        user2.setId("user2");
        user2.setUsername("user2");
        user2.setEmail("user2@test.com");
        user2.setStatus(UserStatus.INACTIVE);
        Set<Role> user2Roles = new HashSet<>();
        user2Roles.add(userRole);
        user2.setRoles(user2Roles);

        // Setup update request
        updateRequest = new UpdateUserRequest();
        updateRequest.setUsername("updatedUser");
        updateRequest.setEmail("updated@test.com");
        updateRequest.setPassword("newPassword123");
        Set<Role> updateRoles = new HashSet<>();
        updateRoles.add(userRole);
        updateRequest.setRoles(updateRoles);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllUsers_Success() throws Exception {
        // Given
        List<User> users = List.of(user1, user2);
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].id").value(user1.getId()))
                .andExpect(jsonPath("$[0].username").value(user1.getUsername()))
                .andExpect(jsonPath("$[1].id").value(user2.getId()))
                .andExpect(jsonPath("$[1].username").value(user2.getUsername()));

        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getUserById_Success() throws Exception {
        // Given
        when(userService.getUserById(user1.getId())).thenReturn(Optional.of(user1));

        // When & Then
        mockMvc.perform(get("/users/{id}", user1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user1.getId()))
                .andExpect(jsonPath("$.username").value(user1.getUsername()))
                .andExpect(jsonPath("$.email").value(user1.getEmail()))
                .andExpect(jsonPath("$.status").value(user1.getStatus().name()));

        verify(userService).getUserById(user1.getId());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getUserById_NotFound() throws Exception {
        // Given
        when(userService.getUserById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/users/{id}", "nonexistent"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService).getUserById("nonexistent");
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getUserByUsername_Success() throws Exception {
        // Given
        when(userService.getUserByUsername(user1.getUsername())).thenReturn(Optional.of(user1));

        // When & Then
        mockMvc.perform(get("/users/username/{username}", user1.getUsername()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user1.getId()))
                .andExpect(jsonPath("$.username").value(user1.getUsername()));

        verify(userService).getUserByUsername(user1.getUsername());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void getUserByEmail_Success() throws Exception {
        // Given
        when(userService.getUserByEmail(user1.getEmail())).thenReturn(Optional.of(user1));

        // When & Then
        mockMvc.perform(get("/users/email/{email}", user1.getEmail()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user1.getId()))
                .andExpect(jsonPath("$.email").value(user1.getEmail()));

        verify(userService).getUserByEmail(user1.getEmail());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void updateUser_Success() throws Exception {
        // Given
        User updatedUser = new User();
        updatedUser.setId(user1.getId());
        updatedUser.setUsername(updateRequest.getUsername());
        updatedUser.setEmail(updateRequest.getEmail());
        updatedUser.setPassword("encodedPassword");
        updatedUser.setRoles(updateRequest.getRoles());

        when(userService.getUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(passwordEncoder.encode(updateRequest.getPassword())).thenReturn("encodedPassword");
        when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userService.updateUser(any(User.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/users/{id}", user1.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user1.getId()))
                .andExpect(jsonPath("$.username").value(updateRequest.getUsername()))
                .andExpect(jsonPath("$.email").value(updateRequest.getEmail()));

        verify(userService).getUserById(user1.getId());
        verify(passwordEncoder).encode(updateRequest.getPassword());
        verify(userService).updateUser(any(User.class));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void updateUser_NotFound() throws Exception {
        // Given
        when(userService.getUserById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/users/{id}", "nonexistent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService).getUserById("nonexistent");
        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUserStatus_Success() throws Exception {
        // Given
        User updatedUser = new User();
        updatedUser.setId(user1.getId());
        updatedUser.setStatus(UserStatus.SUSPENDED);

        when(userService.getUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(userService.updateUser(any(User.class))).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(patch("/users/{id}/status/{status}", user1.getId(), "SUSPENDED")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user1.getId()))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(userService).getUserById(user1.getId());
        verify(userService).updateUser(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateUserStatus_InvalidStatus() throws Exception {
        // Given
        when(userService.getUserById(user1.getId())).thenReturn(Optional.of(user1));

        // When & Then
        mockMvc.perform(patch("/users/{id}/status/{status}", user1.getId(), "INVALID_STATUS")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status value"));

        verify(userService).getUserById(user1.getId());
        verify(userService, never()).updateUser(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_Success() throws Exception {
        // Given
        when(userService.getUserById(user1.getId())).thenReturn(Optional.of(user1));
        doNothing().when(userService).deleteUser(user1.getId());

        // When & Then
        mockMvc.perform(delete("/users/{id}", user1.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).getUserById(user1.getId());
        verify(userService).deleteUser(user1.getId());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_NotFound() throws Exception {
        // Given
        when(userService.getUserById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/users/{id}", "nonexistent")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));

        verify(userService).getUserById("nonexistent");
        verify(userService, never()).deleteUser(anyString());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void deleteUser_Forbidden() throws Exception {
        // Given
        when(userService.getUserById(user1.getId())).thenReturn(Optional.of(user1));

        // When & Then
        mockMvc.perform(delete("/users/{id}", user1.getId())
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(anyString());
    }
}