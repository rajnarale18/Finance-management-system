package com.finance.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * User Entity - Represents a user in the system.
 *
 * Each user has:
 * - A unique username and email
 * - A password (stored in encrypted form)
 * - A role (ADMIN, MANAGER, or USER)
 * - Active status (can be enabled/disabled)
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    // Password will be stored as BCrypt hash (never plain text!)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    // Many users can have the same role
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    // Is this user account active? (ADMIN can deactivate users)
    @Column(nullable = false)
    private boolean active = true;

    // Automatically set when user is created
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
