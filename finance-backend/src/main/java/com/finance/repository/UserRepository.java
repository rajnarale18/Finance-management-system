package com.finance.repository;

import com.finance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Handles all database operations for User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username (used during login)
    Optional<User> findByUsername(String username);

    // Find user by email
    Optional<User> findByEmail(String email);

    // Find all active users
    List<User> findByActiveTrue();

    // Check if a username already exists
    boolean existsByUsername(String username);

    // Check if an email already exists
    boolean existsByEmail(String email);
}
