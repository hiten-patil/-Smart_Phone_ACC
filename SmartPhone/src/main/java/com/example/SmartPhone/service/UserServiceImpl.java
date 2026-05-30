package com.example.SmartPhone.service;

import com.example.SmartPhone.entity.User;
import com.example.SmartPhone.model.RegistrationRequest;
import com.example.SmartPhone.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User registerUser(RegistrationRequest request) throws Exception {
        try {
            // Validate input
            if (request == null) {
                throw new IllegalArgumentException("Registration request cannot be null");
            }
            
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new Exception("Email is required");
            }
            
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                throw new Exception("Username is required");
            }
            
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                throw new Exception("Password is required");
            }
            
            log.info("Validating user registration for: {}", request.getUsername());
            
            // Check for existing email
            if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
                log.warn("Registration failed - email already in use: {}", request.getEmail());
                throw new Exception("Email already in use");
            }
            
            // Check for existing username
            if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
                log.warn("Registration failed - username already in use: {}", request.getUsername());
                throw new Exception("Username already in use");
            }

            User u = new User();
            u.setEmail(request.getEmail().trim());
            
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
                u.setPhoneNumber(request.getPhoneNumber().trim());
            }
            
            // Parse date with error handling
            if (request.getDob() != null && !request.getDob().trim().isEmpty()) {
                try {
                    u.setDob(LocalDate.parse(request.getDob().trim()));
                } catch (DateTimeParseException e) {
                    log.warn("Invalid date format for DOB: {}", request.getDob());
                    throw new Exception("Invalid date format for date of birth");
                }
            }
            
            u.setFirstName(request.getFirstName().trim());
            u.setLastName(request.getLastName().trim());
            u.setUsername(request.getUsername().trim());
            
            // Encode password
            try {
                u.setPassword(passwordEncoder.encode(request.getPassword()));
            } catch (Exception e) {
                log.error("Error encoding password for user: {}", request.getUsername(), e);
                throw new Exception("Error processing password");
            }

            User savedUser = userRepository.save(u);
            log.info("User registered successfully: {}", savedUser.getUsername());
            return savedUser;
            
        } catch (DataAccessException e) {
            log.error("Database error during user registration", e);
            throw new Exception("Database error occurred. Please try again later.");
        } catch (Exception e) {
            if (e.getMessage() != null && 
                (e.getMessage().contains("already in use") || 
                 e.getMessage().contains("required") ||
                 e.getMessage().contains("Invalid date"))) {
                throw e;
            }
            log.error("Unexpected error during user registration", e);
            throw new Exception("Registration failed. Please try again.");
        }
    }

    @Override
    public User findByUsername(String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                log.warn("Attempted to find user with null or empty username");
                return null;
            }
            return userRepository.findByUsername(username.trim()).orElse(null);
        } catch (DataAccessException e) {
            log.error("Database error while finding user: {}", username, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while finding user: {}", username, e);
            return null;
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        try {
            if (userId == null) {
                log.warn("Attempted to delete user with null userId");
                return;
            }
            
            log.info("Deleting user with ID: {}", userId);
            userRepository.deleteById(userId);
            log.info("User deleted successfully with ID: {}", userId);
            
        } catch (DataAccessException e) {
            log.error("Database error while deleting user: {}", userId, e);
            throw new RuntimeException("Failed to delete user. Please try again.");
        } catch (Exception e) {
            log.error("Unexpected error while deleting user: {}", userId, e);
            throw new RuntimeException("An error occurred while deleting the user.");
        }
    }
}
