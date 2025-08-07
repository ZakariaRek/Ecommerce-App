package com.Ecommerce.User_Service;

import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.Role;
import com.Ecommerce.User_Service.Models.ERole;
import com.Ecommerce.User_Service.Models.UserStatus;
import com.Ecommerce.User_Service.Models.UserAddress;
import com.Ecommerce.User_Service.Models.AddressType;
import com.Ecommerce.User_Service.Payload.Request.LoginRequest;
import com.Ecommerce.User_Service.Payload.Request.SignupRequest;
import com.Ecommerce.User_Service.Repositories.UserRepository;
import com.Ecommerce.User_Service.Repositories.RoleRepository;
import com.Ecommerce.User_Service.Repositories.UserAddressRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@AutoConfigureWebMvc
class UserServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.6")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up database
        userAddressRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Setup roles
        userRole = new Role();
        userRole.setName(ERole.ROLE_USER);
        userRole.setDescription("User Role");
        userRole = roleRepository.save(userRole);

        adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);
        adminRole.setDescription("Admin Role");
        adminRole = roleRepository.save(adminRole);
    }

    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
        assertThat(roleRepository).isNotNull();
        assertThat(userAddressRepository).isNotNull();
    }

    @Test
    void shouldInitializeRoles() throws Exception {
        // When
        mockMvc.perform(post("/roles/init")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Roles initialized successfully"));

        // Then - Check roles were created
        assertThat(roleRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(roleRepository.findByName(ERole.ROLE_USER)).isPresent();
        assertThat(roleRepository.findByName(ERole.ROLE_ADMIN)).isPresent();
    }

    @Test
    void shouldRegisterAndAuthenticateUser() throws Exception {
        // Given - Register a new user
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("testuser@test.com");
        signupRequest.setPassword("password123");

        // When - Register user
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        // Then - User should be in database
        Optional<User> savedUser = userRepository.findByUsername("testuser");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo("testuser@test.com");
        assertThat(savedUser.get().getRoles()).hasSize(1);
        assertThat(savedUser.get().getRoles().iterator().next().getName()).isEqualTo(ERole.ROLE_USER);

        // When - Login with created user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@test.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        // Given - Create a user first
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail("existing@test.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        existingUser.setRoles(roles);
        userRepository.save(existingUser);

        // When - Try to register with same username
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("existinguser");
        signupRequest.setEmail("different@test.com");
        signupRequest.setPassword("password123");

        // Then
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already taken!"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void shouldCreateAndRetrieveUserAddress() throws Exception {
        // Given - Create a user first
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        user = userRepository.save(user);

        // Create address request
        UserAddress address = new UserAddress();
        address.setUserId(user.getId());
        address.setAddressType(AddressType.HOME);
        address.setStreet("123 Test Street");
        address.setCity("Test City");
        address.setState("Test State");
        address.setCountry("USA");
        address.setZipCode("12345");
        address.setDefault(true);

        // When - Create address
        mockMvc.perform(post("/addresses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.street").value("123 Test Street"))
                .andExpect(jsonPath("$.city").value("Test City"))
                .andExpect(jsonPath("$.isDefault").value(true));

        // Then - Address should be in database
        assertThat(userAddressRepository.count()).isEqualTo(1);
        Optional<UserAddress> savedAddress = userAddressRepository.findByUserIdAndIsDefaultTrue(user.getId());
        assertThat(savedAddress).isPresent();
        assertThat(savedAddress.get().getStreet()).isEqualTo("123 Test Street");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void shouldRetrieveUserAddressesByUserId() throws Exception {
        // Given - Create a user and addresses
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        user = userRepository.save(user);

        // Create home address
        UserAddress homeAddress = new UserAddress();
        homeAddress.setUserId(user.getId());
        homeAddress.setAddressType(AddressType.HOME);
        homeAddress.setStreet("123 Home St");
        homeAddress.setCity("Home City");
        homeAddress.setState("Home State");
        homeAddress.setCountry("USA");
        homeAddress.setZipCode("12345");
        homeAddress.setDefault(true);
        userAddressRepository.save(homeAddress);

        // Create work address
        UserAddress workAddress = new UserAddress();
        workAddress.setUserId(user.getId());
        workAddress.setAddressType(AddressType.WORK);
        workAddress.setStreet("456 Work Ave");
        workAddress.setCity("Work City");
        workAddress.setState("Work State");
        workAddress.setCountry("USA");
        workAddress.setZipCode("67890");
        workAddress.setDefault(false);
        userAddressRepository.save(workAddress);

        // When - Retrieve user addresses
        mockMvc.perform(get("/addresses/user/{userId}", user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].addressType").exists())
                .andExpect(jsonPath("$[1].addressType").exists());

        // When - Retrieve default address
        mockMvc.perform(get("/addresses/user/{userId}/default", user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("123 Home St"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdateUserStatus() throws Exception {
        // Given - Create a user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setStatus(UserStatus.ACTIVE);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        user = userRepository.save(user);

        // When - Update user status
        mockMvc.perform(patch("/users/{id}/status/{status}", user.getId(), "SUSPENDED")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // Then - User status should be updated in database
        Optional<User> updatedUser = userRepository.findById(user.getId());
        assertThat(updatedUser).isPresent();
        assertThat(updatedUser.get().getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldDeleteUser() throws Exception {
        // Given - Create a user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        user = userRepository.save(user);

        String userId = user.getId();

        // When - Delete user
        mockMvc.perform(delete("/users/{id}", userId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        // Then - User should be removed from database
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void shouldRejectInvalidLoginCredentials() throws Exception {
        // Given - Create a user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setPassword(passwordEncoder.encode("correctpassword"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        userRepository.save(user);

        // When - Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        // Then
        mockMvc.perform(post("/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}