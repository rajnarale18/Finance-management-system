package com.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * SummaryDTO - Data Transfer Object for Transaction Summary/Reports.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummaryDTO {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;           // totalIncome - totalExpense

    private long pendingTransactions;
    private long approvedTransactions;
    private long rejectedTransactions;

    // Breakdown by category: {"Salary": 50000, "Food": 5000, ...}
    private Map<String, BigDecimal> categoryBreakdown;
}
