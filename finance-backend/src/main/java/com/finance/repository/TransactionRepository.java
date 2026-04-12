package com.finance.repository;

import com.finance.model.Transaction;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * TransactionRepository - Handles all database operations for Transaction.
 *
 * BUG FIX: JPQL enum comparisons must use typed enum params, NOT plain string literals.
 * Wrong  : t.type = 'INCOME'                  (string literal - runtime error)
 * Correct: t.type = :type with @Param          (typed enum parameter - works correctly)
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findByType(TransactionType type);
    List<Transaction> findByUserIdAndType(Long userId, TransactionType type);
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    List<Transaction> findByCategory(String category);
    long countByStatus(TransactionStatus status);

    // ---- FIX: Use typed @Param for enums, not string literals ----

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :incomeType AND t.status = :approvedStatus")
    BigDecimal getTotalIncomeByUser(
            @Param("userId") Long userId,
            @Param("incomeType") TransactionType incomeType,
            @Param("approvedStatus") TransactionStatus approvedStatus);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :expenseType AND t.status = :approvedStatus")
    BigDecimal getTotalExpenseByUser(
            @Param("userId") Long userId,
            @Param("expenseType") TransactionType expenseType,
            @Param("approvedStatus") TransactionStatus approvedStatus);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = :incomeType AND t.status = :approvedStatus")
    BigDecimal getTotalIncome(
            @Param("incomeType") TransactionType incomeType,
            @Param("approvedStatus") TransactionStatus approvedStatus);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = :expenseType AND t.status = :approvedStatus")
    BigDecimal getTotalExpense(
            @Param("expenseType") TransactionType expenseType,
            @Param("approvedStatus") TransactionStatus approvedStatus);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
           "WHERE t.status = :approvedStatus GROUP BY t.category")
    List<Object[]> getSummaryByCategory(@Param("approvedStatus") TransactionStatus approvedStatus);
}
