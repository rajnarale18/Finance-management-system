package com.finance.controller;

import com.finance.dto.TransactionDTO;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TransactionController - Handles all HTTP requests related to Transactions.
 *
 * Base URL: /api/transactions
 *
 * Endpoints:
 * POST   /api/transactions                   - Create transaction   (USER, MANAGER, ADMIN)
 * GET    /api/transactions                   - Get all              (MANAGER, ADMIN)
 * GET    /api/transactions/{id}              - Get by ID            (All roles)
 * GET    /api/transactions/user/{userId}     - Get by user          (All roles)
 * GET    /api/transactions/filter?type=...   - Filter by type       (All roles)
 * GET    /api/transactions/filter?status=... - Filter by status     (MANAGER, ADMIN)
 * GET    /api/transactions/filter?start=&end=- Filter by dates      (All roles)
 * PATCH  /api/transactions/{id}/status       - Approve/Reject       (MANAGER, ADMIN)
 * DELETE /api/transactions/{id}              - Delete               (ADMIN)
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // ---- Create Transaction ----
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> createTransaction(@PathVariable Long userId,
                                               @RequestBody TransactionDTO.CreateRequest request) {
        try {
            TransactionDTO.Response created = transactionService.createTransaction(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ---- Get All Transactions ----
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TransactionDTO.Response>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    // ---- Get Transaction by ID ----
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(transactionService.getTransactionById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- Get Transactions by User ----
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<TransactionDTO.Response>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }

    // ---- Filter by Type (INCOME / EXPENSE) ----
    @GetMapping("/filter/type")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<TransactionDTO.Response>> filterByType(
            @RequestParam TransactionType type) {
        return ResponseEntity.ok(transactionService.getByType(type));
    }

    // ---- Filter by Status (PENDING / APPROVED / REJECTED) ----
    @GetMapping("/filter/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<TransactionDTO.Response>> filterByStatus(
            @RequestParam TransactionStatus status) {
        return ResponseEntity.ok(transactionService.getByStatus(status));
    }

    // ---- Filter by Date Range ----
    @GetMapping("/filter/date")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<TransactionDTO.Response>> filterByDateRange(
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(transactionService.getByDateRange(start, end));
    }

    // ---- Approve / Reject Transaction (Manager/Admin only) ----
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody TransactionDTO.StatusUpdateRequest request) {
        try {
            return ResponseEntity.ok(transactionService.updateStatus(id, request.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ---- Delete Transaction (Admin only) ----
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        try {
            transactionService.deleteTransaction(id);
            return ResponseEntity.ok("Transaction deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
