package com.finance.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction Entity - Represents a financial transaction.
 *
 * Each transaction has:
 * - A type (INCOME or EXPENSE)
 * - An amount and currency
 * - A category (e.g., Salary, Rent, Food)
 * - A status (PENDING, APPROVED, REJECTED)
 * - The user who created it
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Type of transaction
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // INCOME or EXPENSE

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Default currency is INR (Indian Rupees)
    @Column(nullable = false)
    private String currency = "INR";

    // Category of the transaction (e.g., Salary, Food, Rent)
    @Column(nullable = false)
    private String category;

    // Short description of what the transaction is for
    private String description;

    // Date when the transaction occurred
    @Column(nullable = false)
    private LocalDate transactionDate;

    // Current status of the transaction
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    // The user who created this transaction
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // When was this record created
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Enums for Transaction ----

    public enum TransactionType {
        INCOME,   // Money coming in
        EXPENSE   // Money going out
    }

    public enum TransactionStatus {
        PENDING,   // Waiting for approval
        APPROVED,  // Approved by Manager/Admin
        REJECTED   // Rejected by Manager/Admin
    }
}
