package com.Ecommerce.User_Service.Services;

import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.UserStatus;
import com.Ecommerce.User_Service.Repositories.UserRepository;
import com.Ecommerce.User_Service.Services.Kafka.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        // Send Kafka event for user creation
        kafkaProducerService.sendUserCreatedEvent(savedUser);

        return savedUser;
    }

    public User updateUser(User user) {
        UserStatus previousStatus = null;

        // Check if the user status is being changed
        Optional<User> existingUserOpt = userRepository.findById(user.getId());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            previousStatus = existingUser.getStatus();
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Send Kafka event for user update
        kafkaProducerService.sendUserUpdatedEvent(updatedUser);

        // If status changed, send a specific status changed event
        if (previousStatus != null && previousStatus != updatedUser.getStatus()) {
            kafkaProducerService.sendUserStatusChangedEvent(updatedUser);
        }

        return updatedUser;
    }

    public void deleteUser(String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userRepository.deleteById(id);

            // Send Kafka event for user deletion
            kafkaProducerService.sendUserDeletedEvent(user);
        }
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}