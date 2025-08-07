package com.Ecommerce.User_Service.Services;

import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Models.ERole;
import com.Ecommerce.User_Service.Models.UserStatus;
import com.Ecommerce.User_Service.Repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId("role1");
        userRole.setName(ERole.ROLE_USER);

        user1 = new User();
        user1.setId("user1");
        user1.setUsername("testuser1");
        user1.setEmail("testuser1@test.com");
        user1.setPassword("password123");
        user1.setStatus(UserStatus.ACTIVE);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user1.setRoles(roles);

        user2 = new User();
        user2.setId("user2");
        user2.setUsername("testuser2");
        user2.setEmail("testuser2@test.com");
        user2.setPassword("password456");
        user2.setStatus(UserStatus.INACTIVE);
        user2.setRoles(roles);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        List<User> expectedUsers = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // When
        List<User> actualUsers = userService.getAllUsers();

        // Then
        assertThat(actualUsers).isEqualTo(expectedUsers);
        assertThat(actualUsers).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));

        // When
        Optional<User> result = userService.getUserById(user1.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user1);
        verify(userRepository).findById(user1.getId());
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Given
        String nonExistentId = "nonexistent";
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(nonExistentId);
    }

    @Test
    void getUserByUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findByUsername(user1.getUsername())).thenReturn(Optional.of(user1));

        // When
        Optional<User> result = userService.getUserByUsername(user1.getUsername());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user1);
        verify(userRepository).findByUsername(user1.getUsername());
    }

    @Test
    void getUserByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Given
        String nonExistentUsername = "nonexistent";
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserByUsername(nonExistentUsername);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername(nonExistentUsername);
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findByEmail(user1.getEmail())).thenReturn(Optional.of(user1));

        // When
        Optional<User> result = userService.getUserByEmail(user1.getEmail());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user1);
        verify(userRepository).findByEmail(user1.getEmail());
    }

    @Test
    void getUserByEmail_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Given
        String nonExistentEmail = "nonexistent@test.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserByEmail(nonExistentEmail);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByEmail(nonExistentEmail);
    }

    @Test
    void updateUser_ShouldUpdateUserAndSetUpdatedAt() {
        // Given
        user1.setUsername("updatedUsername");
        user1.setEmail("updated@test.com");
        when(userRepository.save(any(User.class))).thenReturn(user1);

        // When
        User result = userService.updateUser(user1);

        // Then
        assertThat(result).isEqualTo(user1);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(userRepository).save(user1);
    }

    @Test
    void deleteUser_ShouldCallRepositoryDelete() {
        // Given
        String userId = "user1";
        doNothing().when(userRepository).deleteById(userId);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).deleteById(userId);
    }

    @Test
    void updateUser_ShouldPreserveExistingTimestampsAndUpdateThem() {
        // Given
        User existingUser = new User();
        existingUser.setId("existing");
        existingUser.setUsername("existing");
        existingUser.setEmail("existing@test.com");
        existingUser.setCreatedAt(java.time.LocalDateTime.now().minusDays(1));
        existingUser.setUpdatedAt(java.time.LocalDateTime.now().minusDays(1));

        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User result = userService.updateUser(existingUser);

        // Then
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isAfterOrEqualTo(result.getCreatedAt());
        verify(userRepository).save(existingUser);
    }
}