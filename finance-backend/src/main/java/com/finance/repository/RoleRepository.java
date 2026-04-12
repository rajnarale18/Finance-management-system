package com.finance.repository;

import com.finance.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * RoleRepository - Handles all database operations for Role.
 * JpaRepository gives us free CRUD methods like save(), findById(), findAll(), delete() etc.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Find a role by its name (e.g., "ADMIN")
    Optional<Role> findByName(String name);
}
