package com.finance.service;

import com.finance.dto.TransactionDTO;
import com.finance.model.Role;
import com.finance.model.Transaction;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.model.User;
import com.finance.repository.TransactionRepository;
import com.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionServiceTest - Unit tests for TransactionService.
 *
 * Tests: createTransaction, getAllTransactions, getTransactionsByUser,
 *        getByType, getByStatus, getByDateRange, updateStatus, deleteTransaction.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TransactionService transactionService;

    private User sampleUser;
    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        Role role = new Role(1L, "USER", "Standard user");

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("john");
        sampleUser.setEmail("john@example.com");
        sampleUser.setPassword("encoded");
        sampleUser.setFullName("John Doe");
        sampleUser.setRole(role);
        sampleUser.setActive(true);

        sampleTransaction = new Transaction();
        sampleTransaction.setId(1L);
        sampleTransaction.setType(TransactionType.INCOME);
        sampleTransaction.setAmount(new BigDecimal("50000.00"));
        sampleTransaction.setCurrency("INR");
        sampleTransaction.setCategory("Salary");
        sampleTransaction.setDescription("April salary");
        sampleTransaction.setTransactionDate(LocalDate.of(2024, 4, 1));
        sampleTransaction.setStatus(TransactionStatus.PENDING);
        sampleTransaction.setUser(sampleUser);
        sampleTransaction.setCreatedAt(LocalDateTime.now());
    }

    // ================================================================
    // createTransaction
    // ================================================================

    @Test
    @DisplayName("createTransaction: should create with PENDING status by default")
    void createTransaction_DefaultsPending() {
        TransactionDTO.CreateRequest req = new TransactionDTO.CreateRequest(
                TransactionType.INCOME, new BigDecimal("50000"),
                "INR", "Salary", "April salary", "2024-04-01");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        TransactionDTO.Response result = transactionService.createTransaction(req, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getType()).isEqualTo("INCOME");
        assertThat(result.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(result.getCreatedByUsername()).isEqualTo("john");

        // Verify transaction was saved with PENDING status
        verify(transactionRepository).save(argThat(t ->
                t.getStatus() == TransactionStatus.PENDING));
    }

    @Test
    @DisplayName("createTransaction: should default currency to INR when not provided")
    void createTransaction_DefaultsCurrencyINR() {
        TransactionDTO.CreateRequest req = new TransactionDTO.CreateRequest(
                TransactionType.EXPENSE, new BigDecimal("5000"),
                null, "Food", "Groceries", "2024-04-05");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction);

        transactionService.createTransaction(req, 1L);

        verify(transactionRepository).save(argThat(t ->
                "INR".equals(t.getCurrency())));
    }

    @Test
    @DisplayName("createTransaction: should throw when user not found")
    void createTransaction_UserNotFound_Throws() {
        TransactionDTO.CreateRequest req = new TransactionDTO.CreateRequest(
                TransactionType.INCOME, new BigDecimal("5000"),
                "INR", "Salary", "desc", "2024-04-01");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransaction(req, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with ID: 99");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction: should parse date string correctly")
    void createTransaction_ParsesDateCorrectly() {
        TransactionDTO.CreateRequest req = new TransactionDTO.CreateRequest(
                TransactionType.INCOME, new BigDecimal("1000"),
                "INR", "Bonus", "desc", "2024-12-25");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(transactionRepository.save(any())).thenReturn(sampleTransaction);

        transactionService.createTransaction(req, 1L);

        verify(transactionRepository).save(argThat(t ->
                t.getTransactionDate().equals(LocalDate.of(2024, 12, 25))));
    }

    @Test
    @DisplayName("createTransaction: should throw for invalid date format")
    void createTransaction_InvalidDate_Throws() {
        TransactionDTO.CreateRequest req = new TransactionDTO.CreateRequest(
                TransactionType.INCOME, new BigDecimal("1000"),
                "INR", "Bonus", "desc", "25-12-2024"); // wrong format

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> transactionService.createTransaction(req, 1L))
                .isInstanceOf(Exception.class);
    }

    // ================================================================
    // getAllTransactions
    // ================================================================

    @Test
    @DisplayName("getAllTransactions: should return all transactions")
    void getAllTransactions_ReturnsList() {
        Transaction t2 = new Transaction();
        t2.setId(2L); t2.setType(TransactionType.EXPENSE);
        t2.setAmount(new BigDecimal("3000")); t2.setCurrency("INR");
        t2.setCategory("Food"); t2.setStatus(TransactionStatus.APPROVED);
        t2.setTransactionDate(LocalDate.now()); t2.setUser(sampleUser);

        when(transactionRepository.findAll()).thenReturn(List.of(sampleTransaction, t2));

        List<TransactionDTO.Response> result = transactionService.getAllTransactions();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getAllTransactions: should return empty list when none exist")
    void getAllTransactions_Empty() {
        when(transactionRepository.findAll()).thenReturn(List.of());
        assertThat(transactionService.getAllTransactions()).isEmpty();
    }

    // ================================================================
    // getTransactionsByUser
    // ================================================================

    @Test
    @DisplayName("getTransactionsByUser: should return only that user's transactions")
    void getTransactionsByUser_ReturnsUserTransactions() {
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of(sampleTransaction));

        List<TransactionDTO.Response> result = transactionService.getTransactionsByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCreatedByUsername()).isEqualTo("john");
    }

    @Test
    @DisplayName("getTransactionsByUser: should return empty list when user has no transactions")
    void getTransactionsByUser_Empty() {
        when(transactionRepository.findByUserId(2L)).thenReturn(List.of());
        assertThat(transactionService.getTransactionsByUser(2L)).isEmpty();
    }

    // ================================================================
    // getByType
    // ================================================================

    @Test
    @DisplayName("getByType: should return only INCOME transactions")
    void getByType_Income() {
        when(transactionRepository.findByType(TransactionType.INCOME))
                .thenReturn(List.of(sampleTransaction));

        List<TransactionDTO.Response> result = transactionService.getByType(TransactionType.INCOME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("INCOME");
    }

    @Test
    @DisplayName("getByType: should return only EXPENSE transactions")
    void getByType_Expense() {
        Transaction expense = new Transaction();
        expense.setId(2L); expense.setType(TransactionType.EXPENSE);
        expense.setAmount(new BigDecimal("2000")); expense.setCurrency("INR");
        expense.setCategory("Rent"); expense.setStatus(TransactionStatus.PENDING);
        expense.setTransactionDate(LocalDate.now()); expense.setUser(sampleUser);

        when(transactionRepository.findByType(TransactionType.EXPENSE))
                .thenReturn(List.of(expense));

        List<TransactionDTO.Response> result = transactionService.getByType(TransactionType.EXPENSE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("EXPENSE");
    }

    // ================================================================
    // getByStatus
    // ================================================================

    @Test
    @DisplayName("getByStatus: should return only PENDING transactions")
    void getByStatus_Pending() {
        when(transactionRepository.findByStatus(TransactionStatus.PENDING))
                .thenReturn(List.of(sampleTransaction));

        List<TransactionDTO.Response> result = transactionService.getByStatus(TransactionStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
    }

    // ================================================================
    // getByDateRange
    // ================================================================

    @Test
    @DisplayName("getByDateRange: should return transactions within date range")
    void getByDateRange_ReturnsFiltered() {
        when(transactionRepository.findByTransactionDateBetween(
                LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 30)))
                .thenReturn(List.of(sampleTransaction));

        List<TransactionDTO.Response> result =
                transactionService.getByDateRange("2024-04-01", "2024-04-30");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTransactionDate()).isEqualTo("2024-04-01");
    }

    @Test
    @DisplayName("getByDateRange: should return empty when no transactions in range")
    void getByDateRange_Empty() {
        when(transactionRepository.findByTransactionDateBetween(any(), any()))
                .thenReturn(List.of());

        assertThat(transactionService.getByDateRange("2023-01-01", "2023-01-31")).isEmpty();
    }

    // ================================================================
    // getTransactionById
    // ================================================================

    @Test
    @DisplayName("getTransactionById: should return transaction when found")
    void getTransactionById_Found() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));

        TransactionDTO.Response result = transactionService.getTransactionById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCategory()).isEqualTo("Salary");
    }

    @Test
    @DisplayName("getTransactionById: should throw when not found")
    void getTransactionById_NotFound_Throws() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found with ID: 99");
    }

    // ================================================================
    // updateStatus (Approve / Reject)
    // ================================================================

    @Test
    @DisplayName("updateStatus: should approve a PENDING transaction")
    void updateStatus_Approve() {
        Transaction approved = new Transaction();
        approved.setId(1L); approved.setType(TransactionType.INCOME);
        approved.setAmount(new BigDecimal("50000")); approved.setCurrency("INR");
        approved.setCategory("Salary"); approved.setStatus(TransactionStatus.APPROVED);
        approved.setTransactionDate(LocalDate.of(2024, 4, 1)); approved.setUser(sampleUser);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any())).thenReturn(approved);

        TransactionDTO.Response result = transactionService.updateStatus(1L, TransactionStatus.APPROVED);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(transactionRepository).save(argThat(t -> t.getStatus() == TransactionStatus.APPROVED));
    }

    @Test
    @DisplayName("updateStatus: should reject a PENDING transaction")
    void updateStatus_Reject() {
        Transaction rejected = new Transaction();
        rejected.setId(1L); rejected.setType(TransactionType.INCOME);
        rejected.setAmount(new BigDecimal("50000")); rejected.setCurrency("INR");
        rejected.setCategory("Salary"); rejected.setStatus(TransactionStatus.REJECTED);
        rejected.setTransactionDate(LocalDate.of(2024, 4, 1)); rejected.setUser(sampleUser);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(sampleTransaction));
        when(transactionRepository.save(any())).thenReturn(rejected);

        TransactionDTO.Response result = transactionService.updateStatus(1L, TransactionStatus.REJECTED);
        assertThat(result.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("updateStatus: should throw when transaction not found")
    void updateStatus_NotFound_Throws() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> transactionService.updateStatus(99L, TransactionStatus.APPROVED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found with ID: 99");
    }

    // ================================================================
    // deleteTransaction
    // ================================================================

    @Test
    @DisplayName("deleteTransaction: should call deleteById when transaction exists")
    void deleteTransaction_Success() {
        when(transactionRepository.existsById(1L)).thenReturn(true);

        transactionService.deleteTransaction(1L);

        verify(transactionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTransaction: should throw when transaction not found")
    void deleteTransaction_NotFound_Throws() {
        when(transactionRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.deleteTransaction(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found with ID: 99");

        verify(transactionRepository, never()).deleteById(any());
    }
}
