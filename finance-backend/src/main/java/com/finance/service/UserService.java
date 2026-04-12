package com.finance.service;

import com.finance.dto.UserDTO;
import com.finance.model.Role;
import com.finance.model.User;
import com.finance.repository.RoleRepository;
import com.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService - Contains all business logic related to Users.
 * The controller calls the service, and the service calls the repository.
 * Controller -> Service -> Repository -> Database
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Used to encrypt passwords

    // ---- Create a new user ----
    public UserDTO.Response createUser(UserDTO.CreateRequest request) {

        // Check if username already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        // Check if email already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // Find the role or throw error
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));

        // Create new user object
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Encrypt password
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setActive(true);

        // Save to database
        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    // ---- Get all users (ADMIN only) ----
    public List<UserDTO.Response> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---- Get user by ID ----
    public UserDTO.Response getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return mapToResponse(user);
    }

    // ---- Update user ----
    public UserDTO.Response updateUser(Long id, UserDTO.UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Update only if new value is provided
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getActive() != null) user.setActive(request.getActive());
        if (request.getRoleName() != null) {
            Role role = roleRepository.findByName(request.getRoleName())
                    .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));
            user.setRole(role);
        }

        return mapToResponse(userRepository.save(user));
    }

    // ---- Delete (deactivate) user ----
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setActive(false); // Soft delete: just deactivate instead of removing
        userRepository.save(user);
    }

    // ---- Helper: convert User entity to Response DTO ----
    private UserDTO.Response mapToResponse(User user) {
        UserDTO.Response response = new UserDTO.Response();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRoleName(user.getRole() != null ? user.getRole().getName() : "N/A");
        response.setActive(user.isActive());
        response.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return response;
    }
}
