package com.easyops.users.service;

import com.easyops.users.dto.UserCreateRequest;
import com.easyops.users.dto.UserResponse;
import com.easyops.users.dto.UserUpdateRequest;
import com.easyops.users.entity.User;
import com.easyops.users.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * User Service
 * 
 * Service class for user management operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new user
     * 
     * @param request User creation request
     * @return Created user response
     */
    public UserResponse createUser(UserCreateRequest request) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        String email = request.getEmail();
        if (email != null && userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setIsActive(true);
        user.setIsVerified(false);

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    /**
     * Get user by ID
     * 
     * @param id User ID
     * @return User response
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        return mapToUserResponse(user);
    }

    /**
     * Get all users with pagination
     * 
     * @param pageable Pagination parameters
     * @return Page of user responses
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Update user
     * 
     * @param id User ID
     * @param request User update request
     * @return Updated user response
     */
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));

        // Update user fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    /**
     * Delete user
     * 
     * @param id User ID
     */
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }
        userRepository.deleteById(id);
    }

    /**
     * Activate user
     * 
     * @param id User ID
     * @return Updated user response
     */
    public UserResponse activateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    /**
     * Deactivate user
     * 
     * @param id User ID
     * @return Updated user response
     */
    public UserResponse deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        user.setIsActive(false);
        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    /**
     * Search users
     * 
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of user responses
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchUsers(searchTerm, pageable)
                .map(this::mapToUserResponse);
    }

    /**
     * Get user statistics
     * 
     * @return User statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByIsActiveTrue());
        stats.put("inactiveUsers", userRepository.count() - userRepository.countByIsActiveTrue());
        return stats;
    }

    /**
     * Map User entity to UserResponse DTO
     * 
     * @param user User entity
     * @return UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setIsActive(user.getIsActive());
        response.setIsVerified(user.getIsVerified());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
    }
}
