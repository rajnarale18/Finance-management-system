package com.finance.service;

import com.finance.dto.SummaryDTO;
import com.finance.model.Transaction.TransactionStatus;
import com.finance.model.Transaction.TransactionType;
import com.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SummaryService - Generates financial summaries/reports.
 *
 * BUG FIX: Added null-safety for BigDecimal values.
 * When no transactions exist, COALESCE returns 0, but we also guard with
 * null checks to prevent NullPointerException on .subtract() calls.
 *
 * BUG FIX: Updated method calls to match fixed TransactionRepository signatures.
 */
@Service
public class SummaryService {

    @Autowired
    private TransactionRepository transactionRepository;

    // ---- Overall Summary (for Admin/Manager) ----
    public SummaryDTO getOverallSummary() {

        // FIX: Pass enum constants as parameters + null-safe defaults
        BigDecimal totalIncome = orZero(transactionRepository.getTotalIncome(
                TransactionType.INCOME, TransactionStatus.APPROVED));

        BigDecimal totalExpense = orZero(transactionRepository.getTotalExpense(
                TransactionType.EXPENSE, TransactionStatus.APPROVED));

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        long pending  = transactionRepository.countByStatus(TransactionStatus.PENDING);
        long approved = transactionRepository.countByStatus(TransactionStatus.APPROVED);
        long rejected = transactionRepository.countByStatus(TransactionStatus.REJECTED);

        Map<String, BigDecimal> categoryBreakdown = getCategoryBreakdown();

        return new SummaryDTO(totalIncome, totalExpense, netBalance,
                              pending, approved, rejected, categoryBreakdown);
    }

    // ---- Per-User Summary ----
    public SummaryDTO getUserSummary(Long userId) {

        BigDecimal totalIncome = orZero(transactionRepository.getTotalIncomeByUser(
                userId, TransactionType.INCOME, TransactionStatus.APPROVED));

        BigDecimal totalExpense = orZero(transactionRepository.getTotalExpenseByUser(
                userId, TransactionType.EXPENSE, TransactionStatus.APPROVED));

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        // FIX: Query once, then count by status in memory (avoids 3 extra DB hits)
        List<com.finance.model.Transaction> userTxns = transactionRepository.findByUserId(userId);
        long pending  = userTxns.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long approved = userTxns.stream().filter(t -> t.getStatus() == TransactionStatus.APPROVED).count();
        long rejected = userTxns.stream().filter(t -> t.getStatus() == TransactionStatus.REJECTED).count();

        return new SummaryDTO(totalIncome, totalExpense, netBalance,
                              pending, approved, rejected, null);
    }

    // ---- Category Breakdown ----
    private Map<String, BigDecimal> getCategoryBreakdown() {
        List<Object[]> results = transactionRepository.getSummaryByCategory(TransactionStatus.APPROVED);
        Map<String, BigDecimal> breakdown = new HashMap<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            breakdown.put(category, total);
        }
        return breakdown;
    }

    // ---- Helper: return BigDecimal.ZERO if value is null ----
    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
