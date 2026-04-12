package com.finance.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Role Entity - Represents user roles in the system.
 *
 * We have 3 roles:
 * - ADMIN  : Full access (manage users, view all transactions)
 * - MANAGER: Can view and approve transactions
 * - USER   : Can only create and view own transactions
 */
@Entity
@Table(name = "roles")
@Data                   // Lombok: auto-generates getters, setters, toString, equals, hashCode
@NoArgsConstructor      // Lombok: auto-generates empty constructor
@AllArgsConstructor     // Lombok: auto-generates constructor with all fields
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Role name, e.g., "ADMIN", "MANAGER", "USER"
    @Column(nullable = false, unique = true)
    private String name;

    // Description of what this role can do
    private String description;
}
