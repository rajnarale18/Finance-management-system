package com.finance.service;

import com.finance.dto.TransactionDTO;
import com.finance.model.Transaction;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.model.User;
import com.finance.repository.TransactionRepository;
import com.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionService - Contains all business logic for Transactions.
 */
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    // ---- Create a new transaction ----
    public TransactionDTO.Response createTransaction(TransactionDTO.CreateRequest request, Long userId) {

        // Find the user creating the transaction
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Build transaction object
        Transaction transaction = new Transaction();
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(LocalDate.parse(request.getTransactionDate()));
        transaction.setStatus(TransactionStatus.PENDING); // Default: always starts as PENDING
        transaction.setUser(user);

        return mapToResponse(transactionRepository.save(transaction));
    }

    // ---- Get ALL transactions (ADMIN/MANAGER only) ----
    public List<TransactionDTO.Response> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---- Get transactions by a specific user ----
    public List<TransactionDTO.Response> getTransactionsByUser(Long userId) {
        return transactionRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---- Get transactions filtered by type ----
    public List<TransactionDTO.Response> getByType(TransactionType type) {
        return transactionRepository.findByType(type)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---- Get transactions filtered by status ----
    public List<TransactionDTO.Response> getByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---- Get transactions by date range ----
    public List<TransactionDTO.Response> getByDateRange(String startDate, String endDate) {
        return transactionRepository
                .findByTransactionDateBetween(LocalDate.parse(startDate), LocalDate.parse(endDate))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---- Get transaction by ID ----
    public TransactionDTO.Response getTransactionById(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + id));
        return mapToResponse(t);
    }

    // ---- Approve or Reject transaction (MANAGER/ADMIN only) ----
    public TransactionDTO.Response updateStatus(Long id, TransactionStatus status) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + id));

        transaction.setStatus(status);
        return mapToResponse(transactionRepository.save(transaction));
    }

    // ---- Delete transaction ----
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("Transaction not found with ID: " + id);
        }
        transactionRepository.deleteById(id);
    }

    // ---- Helper: convert Transaction entity to Response DTO ----
    private TransactionDTO.Response mapToResponse(Transaction t) {
        TransactionDTO.Response response = new TransactionDTO.Response();
        response.setId(t.getId());
        response.setType(t.getType().name());
        response.setAmount(t.getAmount());
        response.setCurrency(t.getCurrency());
        response.setCategory(t.getCategory());
        response.setDescription(t.getDescription());
        response.setTransactionDate(t.getTransactionDate().toString());
        response.setStatus(t.getStatus().name());
        response.setCreatedByUsername(t.getUser() != null ? t.getUser().getUsername() : "Unknown");
        response.setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        return response;
    }
}
