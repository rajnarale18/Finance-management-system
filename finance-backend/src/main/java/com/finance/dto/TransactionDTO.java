package com.finance.dto;

import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import lombok.*;
import java.math.BigDecimal;

/**
 * TransactionDTO - Data Transfer Object for Transaction.
 */
public class TransactionDTO {

    // ---- Request DTO (creating a new transaction) ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private TransactionType type;       // INCOME or EXPENSE
        private BigDecimal amount;
        private String currency;            // e.g., "INR", "USD"
        private String category;            // e.g., "Salary", "Food", "Rent"
        private String description;
        private String transactionDate;     // Format: "2024-01-15"
    }

    // ---- Status Update DTO (Manager/Admin approves or rejects) ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusUpdateRequest {
        private TransactionStatus status;  // APPROVED or REJECTED
    }

    // ---- Response DTO ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String type;
        private BigDecimal amount;
        private String currency;
        private String category;
        private String description;
        private String transactionDate;
        private String status;
        private String createdByUsername;  // Who created this transaction
        private String createdAt;
    }
}
