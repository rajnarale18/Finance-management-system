package com.finance.service;

import com.finance.dto.SummaryDTO;
import com.finance.model.Role;
import com.finance.model.Transaction;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.model.User;
import com.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SummaryServiceTest - Unit tests for SummaryService.
 *
 * Key scenarios tested:
 * - Normal summary with data
 * - Null-safety: no crash when DB returns null (empty tables)
 * - Net balance calculated correctly (income - expense)
 * - Per-user summary isolation
 * - Category breakdown grouping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryService Tests")
class SummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private SummaryService summaryService;

    private User sampleUser;

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
    }

    // helper to build a transaction
    private Transaction buildTxn(Long id, TransactionType type, BigDecimal amount,
                                  String category, TransactionStatus status) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setType(type);
        t.setAmount(amount);
        t.setCurrency("INR");
        t.setCategory(category);
        t.setStatus(status);
        t.setTransactionDate(LocalDate.of(2024, 4, 1));
        t.setUser(sampleUser);
        return t;
    }

    // ================================================================
    // getOverallSummary
    // ================================================================

    @Test
    @DisplayName("getOverallSummary: should calculate correct income, expense and net balance")
    void getOverallSummary_CorrectCalculations() {
        when(transactionRepository.getTotalIncome(TransactionType.INCOME, TransactionStatus.APPROVED))
                .thenReturn(new BigDecimal("95000.00"));
        when(transactionRepository.getTotalExpense(TransactionType.EXPENSE, TransactionStatus.APPROVED))
                .thenReturn(new BigDecimal("20000.00"));
        when(transactionRepository.countByStatus(TransactionStatus.PENDING)).thenReturn(2L);
        when(transactionRepository.countByStatus(TransactionStatus.APPROVED)).thenReturn(3L);
        when(transactionRepository.countByStatus(TransactionStatus.REJECTED)).thenReturn(1L);
        when(transactionRepository.getSummaryByCategory(TransactionStatus.APPROVED))
                .thenReturn(List.of());

        SummaryDTO result = summaryService.getOverallSummary();

        assertThat(result.getTotalIncome()).isEqualByComparingTo("95000.00");
        assertThat(result.getTotalExpense()).isEqualByComparingTo("20000.00");
        assertThat(result.getNetBalance()).isEqualByComparingTo("75000.00"); // 95000 - 20000
        assertThat(result.getPendingTransactions()).isEqualTo(2L);
        assertThat(result.getApprovedTransactions()).isEqualTo(3L);
        assertThat(result.getRejectedTransactions()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getOverallSummary: should NOT throw NullPointerException when DB returns null (empty tables)")
    void getOverallSummary_NullFromDB_DoesNotThrow() {
        // Simulates empty DB where COALESCE still returns null in some Hibernate versions
        when(transactionRepository.getTotalIncome(any(), any())).thenReturn(null);
        when(transactionRepository.getTotalExpense(any(), any())).thenReturn(null);
        when(transactionRepository.countByStatus(any())).thenReturn(0L);
        when(transactionRepository.getSummaryByCategory(any())).thenReturn(List.of());

        // This MUST NOT throw NullPointerException
        assertThatCode(() -> summaryService.getOverallSummary())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getOverallSummary: should return ZERO values when no transactions exist")
    void getOverallSummary_EmptyDB_ReturnsZeros() {
        when(transactionRepository.getTotalIncome(any(), any())).thenReturn(null);
        when(transactionRepository.getTotalExpense(any(), any())).thenReturn(null);
        when(transactionRepository.countByStatus(any())).thenReturn(0L);
        when(transactionRepository.getSummaryByCategory(any())).thenReturn(List.of());

        SummaryDTO result = summaryService.getOverallSummary();

        assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getNetBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getPendingTransactions()).isZero();
        assertThat(result.getApprovedTransactions()).isZero();
        assertThat(result.getRejectedTransactions()).isZero();
    }

    @Test
    @DisplayName("getOverallSummary: netBalance should be NEGATIVE when expenses exceed income")
    void getOverallSummary_NegativeBalance() {
        when(transactionRepository.getTotalIncome(any(), any()))
                .thenReturn(new BigDecimal("10000.00"));
        when(transactionRepository.getTotalExpense(any(), any()))
                .thenReturn(new BigDecimal("30000.00"));
        when(transactionRepository.countByStatus(any())).thenReturn(0L);
        when(transactionRepository.getSummaryByCategory(any())).thenReturn(List.of());

        SummaryDTO result = summaryService.getOverallSummary();

        assertThat(result.getNetBalance()).isEqualByComparingTo("-20000.00");
        assertThat(result.getNetBalance()).isNegative();
    }

    @Test
    @DisplayName("getOverallSummary: should build category breakdown map correctly")
    void getOverallSummary_CategoryBreakdown() {
        when(transactionRepository.getTotalIncome(any(), any()))
                .thenReturn(new BigDecimal("50000"));
        when(transactionRepository.getTotalExpense(any(), any()))
                .thenReturn(new BigDecimal("20000"));
        when(transactionRepository.countByStatus(any())).thenReturn(1L);

        // Simulate DB returning category breakdown rows
        Object[] salaryRow  = new Object[]{"Salary", new BigDecimal("50000")};
        Object[] rentRow    = new Object[]{"Rent",   new BigDecimal("12000")};
        Object[] foodRow    = new Object[]{"Food",   new BigDecimal("8000")};
        when(transactionRepository.getSummaryByCategory(TransactionStatus.APPROVED))
                .thenReturn(List.of(salaryRow, rentRow, foodRow));

        SummaryDTO result = summaryService.getOverallSummary();

        assertThat(result.getCategoryBreakdown()).isNotNull();
        assertThat(result.getCategoryBreakdown()).hasSize(3);
        assertThat(result.getCategoryBreakdown().get("Salary"))
                .isEqualByComparingTo("50000");
        assertThat(result.getCategoryBreakdown().get("Rent"))
                .isEqualByComparingTo("12000");
        assertThat(result.getCategoryBreakdown().get("Food"))
                .isEqualByComparingTo("8000");
    }

    @Test
    @DisplayName("getOverallSummary: should call repo with correct APPROVED enum param")
    void getOverallSummary_UsesApprovedStatusForTotals() {
        when(transactionRepository.getTotalIncome(any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalExpense(any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByStatus(any())).thenReturn(0L);
        when(transactionRepository.getSummaryByCategory(any())).thenReturn(List.of());

        summaryService.getOverallSummary();

        // Must query APPROVED transactions for financial totals
        verify(transactionRepository).getTotalIncome(
                eq(TransactionType.INCOME), eq(TransactionStatus.APPROVED));
        verify(transactionRepository).getTotalExpense(
                eq(TransactionType.EXPENSE), eq(TransactionStatus.APPROVED));
    }

    // ================================================================
    // getUserSummary
    // ================================================================

    @Test
    @DisplayName("getUserSummary: should return correct totals for a specific user")
    void getUserSummary_CorrectTotals() {
        List<Transaction> userTxns = List.of(
                buildTxn(1L, TransactionType.INCOME,  new BigDecimal("50000"), "Salary",  TransactionStatus.APPROVED),
                buildTxn(2L, TransactionType.EXPENSE, new BigDecimal("12000"), "Rent",    TransactionStatus.APPROVED),
                buildTxn(3L, TransactionType.EXPENSE, new BigDecimal("3000"),  "Food",    TransactionStatus.PENDING),
                buildTxn(4L, TransactionType.EXPENSE, new BigDecimal("500"),   "Utility", TransactionStatus.REJECTED)
        );

        when(transactionRepository.getTotalIncomeByUser(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("50000.00"));
        when(transactionRepository.getTotalExpenseByUser(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("12000.00"));
        when(transactionRepository.findByUserId(1L)).thenReturn(userTxns);

        SummaryDTO result = summaryService.getUserSummary(1L);

        assertThat(result.getTotalIncome()).isEqualByComparingTo("50000.00");
        assertThat(result.getTotalExpense()).isEqualByComparingTo("12000.00");
        assertThat(result.getNetBalance()).isEqualByComparingTo("38000.00");
        assertThat(result.getPendingTransactions()).isEqualTo(1L);
        assertThat(result.getApprovedTransactions()).isEqualTo(2L);
        assertThat(result.getRejectedTransactions()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUserSummary: should NOT throw when DB returns null for user totals")
    void getUserSummary_NullFromDB_DoesNotThrow() {
        when(transactionRepository.getTotalIncomeByUser(any(), any(), any())).thenReturn(null);
        when(transactionRepository.getTotalExpenseByUser(any(), any(), any())).thenReturn(null);
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of());

        assertThatCode(() -> summaryService.getUserSummary(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getUserSummary: should return zero counts when user has no transactions")
    void getUserSummary_NoTransactions_AllZero() {
        when(transactionRepository.getTotalIncomeByUser(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalExpenseByUser(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of());

        SummaryDTO result = summaryService.getUserSummary(1L);

        assertThat(result.getPendingTransactions()).isZero();
        assertThat(result.getApprovedTransactions()).isZero();
        assertThat(result.getRejectedTransactions()).isZero();
        assertThat(result.getNetBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getUserSummary: categoryBreakdown should be null (per-user summary doesn't include it)")
    void getUserSummary_CategoryBreakdownIsNull() {
        when(transactionRepository.getTotalIncomeByUser(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalExpenseByUser(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findByUserId(1L)).thenReturn(List.of());

        SummaryDTO result = summaryService.getUserSummary(1L);

        // Per-user summary intentionally does not include category breakdown
        assertThat(result.getCategoryBreakdown()).isNull();
    }

    @Test
    @DisplayName("getUserSummary: counts only this user's PENDING/APPROVED/REJECTED — not all users")
    void getUserSummary_CountsOnlyThisUsersTransactions() {
        // User 1 has 1 pending
        List<Transaction> user1Txns = List.of(
                buildTxn(1L, TransactionType.INCOME, new BigDecimal("1000"), "Salary", TransactionStatus.PENDING)
        );

        when(transactionRepository.getTotalIncomeByUser(eq(1L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalExpenseByUser(eq(1L), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findByUserId(1L)).thenReturn(user1Txns);

        SummaryDTO result = summaryService.getUserSummary(1L);

        assertThat(result.getPendingTransactions()).isEqualTo(1L);
        // Ensure we never called countByStatus (which counts ALL users)
        verify(transactionRepository, never()).countByStatus(any());
    }
}
